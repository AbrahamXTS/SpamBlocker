package net.abrahamespinosa.spam_blocker.ui.home

import android.content.ContentValues
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import net.abrahamespinosa.spam_blocker.SpamBlockerApplication
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity
import net.abrahamespinosa.spam_blocker.notifications.InboxNotification
import net.abrahamespinosa.spam_blocker.setup.SetupStateReader

private enum class HomeTab(val title: String) {
    INBOX("Bandeja"),
    QUARANTINE("Cuarentena"),
    KEYWORDS("Palabras"),
    STATUS("Estado"),
}

enum class QuarantineFilter(
    val label: String,
    val type: String,
    val noun: String,
    val emptyLabel: String,
    val adjective: String,
    val quantity: String,
) {
    CALLS("Llamadas", "call", "llamadas", "llamadas bloqueadas", "bloqueadas", "todas las"),
    MESSAGES("Mensajes", "sms", "mensajes", "mensajes bloqueados", "bloqueados", "todos los"),
}

@Composable
fun HomeScreen(
    stateReader: SetupStateReader,
    onRequestRole: (String) -> Unit,
    onRequestContacts: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SpamBlockerApplication
    val database = application.database
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(HomeTab.INBOX) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var selectedQuarantineAddress by remember { mutableStateOf<String?>(null) }
    var selectedQuarantineFilter by remember { mutableStateOf(QuarantineFilter.CALLS) }

    BackHandler(enabled = selectedAddress != null || selectedQuarantineAddress != null) {
        selectedAddress = null
        selectedQuarantineAddress = null
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            selectedAddress = null
                            selectedQuarantineAddress = null
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    HomeTab.INBOX -> Icons.Default.Inbox
                                    HomeTab.QUARANTINE -> Icons.Default.Security
                                    HomeTab.KEYWORDS -> Icons.Default.Key
                                    HomeTab.STATUS -> Icons.Default.Settings
                                },
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                HomeTab.INBOX -> if (selectedAddress == null) {
                    InboxScreen(
                        messages = database.inboxMessageDao().observeAll(),
                        onOpenThread = { selectedAddress = it },
                    )
                } else {
                    ThreadScreen(
                        address = selectedAddress!!,
                        messages = database.inboxMessageDao().observeAll(),
                        onBack = { selectedAddress = null },
                    )
                }

                HomeTab.QUARANTINE -> if (selectedQuarantineAddress == null) {
                    QuarantineScreen(
                        items = database.quarantineDao().observeAll(),
                        selectedFilter = selectedQuarantineFilter,
                        onFilterChange = { selectedQuarantineFilter = it },
                        onOpenSms = { selectedQuarantineAddress = it.address },
                        onDiscard = { item -> scope.launch { database.quarantineDao().delete(item) } },
                        onDeleteAll = { type -> scope.launch { database.quarantineDao().deleteByType(type) } },
                        onRestoreSms = { item ->
                            scope.launch {
                                context.contentResolver.insert(
                                    Telephony.Sms.Inbox.CONTENT_URI,
                                    ContentValues().apply {
                                        put(Telephony.Sms.ADDRESS, item.address)
                                        put(Telephony.Sms.BODY, item.body.orEmpty())
                                        put(Telephony.Sms.DATE, item.receivedAt)
                                        put(Telephony.Sms.READ, 0)
                                    },
                                )
                                database.inboxMessageDao().insertIgnore(
                                    InboxMessageEntity(
                                        address = item.address,
                                        body = item.body.orEmpty(),
                                        receivedAt = item.receivedAt,
                                    ),
                                )
                                database.quarantineDao().delete(item)
                                InboxNotification.show(context, item.address, item.body.orEmpty())
                            }
                        },
                    )
                } else {
                    QuarantineThreadScreen(
                        address = selectedQuarantineAddress!!,
                        items = database.quarantineDao().observeAll(),
                        onBack = { selectedQuarantineAddress = null },
                        onDeleteConversation = { address ->
                            scope.launch {
                                database.quarantineDao().deleteSmsConversation(address)
                                selectedQuarantineAddress = null
                            }
                        },
                    )
                }

                HomeTab.KEYWORDS -> KeywordsScreen(
                    keywords = database.keywordDao().observeAll(),
                    onAdd = { word -> scope.launch { database.keywordDao().insert(word) } },
                    onDelete = { keyword -> scope.launch { database.keywordDao().delete(keyword) } },
                )

                HomeTab.STATUS -> StatusScreen(stateReader, onRequestRole, onRequestContacts)
            }
        }
    }
}
