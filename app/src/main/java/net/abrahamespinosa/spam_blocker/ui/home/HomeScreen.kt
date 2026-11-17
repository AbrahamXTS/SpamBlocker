package net.abrahamespinosa.spam_blocker.ui.home

import android.content.ContentValues
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.abrahamespinosa.spam_blocker.SpamBlockerApplication
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity
import net.abrahamespinosa.spam_blocker.notifications.InboxNotification
import net.abrahamespinosa.spam_blocker.setup.SetupStateReader

internal enum class HomeTab(val title: String) {
    INBOX("Bandeja"),
    QUARANTINE("Cuarentena"),
    KEYWORDS("Palabras"),
    STATUS("Estado"),
}

internal enum class SwipeDirection {
    LEFT,
    RIGHT,
}

internal fun HomeTab.swipeTo(direction: SwipeDirection): HomeTab? {
    return when (direction) {
        SwipeDirection.LEFT -> when (this) {
            HomeTab.INBOX -> HomeTab.QUARANTINE
            HomeTab.QUARANTINE -> HomeTab.KEYWORDS
            HomeTab.KEYWORDS -> HomeTab.STATUS
            HomeTab.STATUS -> null
        }

        SwipeDirection.RIGHT -> when (this) {
            HomeTab.STATUS -> HomeTab.KEYWORDS
            HomeTab.KEYWORDS -> HomeTab.QUARANTINE
            HomeTab.QUARANTINE -> HomeTab.INBOX
            HomeTab.INBOX -> null
        }
    }
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
    openThreadAddress: String?,
    openThreadRequestId: Long,
) {
    val context = LocalContext.current
    val application = context.applicationContext as SpamBlockerApplication
    val database = application.database
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(HomeTab.INBOX) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var selectedQuarantineAddress by remember { mutableStateOf<String?>(null) }
    var selectedQuarantineFilter by remember { mutableStateOf(QuarantineFilter.CALLS) }
    var unavailableThreadNotice by remember { mutableStateOf(false) }
    var transitionDirection by remember { mutableStateOf(SwipeDirection.LEFT) }

    fun selectTab(tab: HomeTab) {
        transitionDirection = if (tab.ordinal > selectedTab.ordinal) {
            SwipeDirection.LEFT
        } else {
            SwipeDirection.RIGHT
        }
        selectedTab = tab
        selectedAddress = null
        selectedQuarantineAddress = null
    }

    fun swipeToTab(direction: SwipeDirection) {
        selectedTab.swipeTo(direction)?.let(::selectTab)
    }

    LaunchedEffect(openThreadAddress, openThreadRequestId) {
        val address = openThreadAddress ?: return@LaunchedEffect
        val messages = database.inboxMessageDao().observeAll().first()
        if (messages.any { conversationKey(it.address) == conversationKey(address) }) {
            selectedTab = HomeTab.INBOX
            selectedAddress = address
            selectedQuarantineAddress = null
        } else {
            selectedTab = HomeTab.INBOX
            selectedAddress = null
            selectedQuarantineAddress = null
            unavailableThreadNotice = true
        }
    }

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
                        onClick = { selectTab(tab) },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .swipeBetweenHomeTabs(selectedTab, ::swipeToTab),
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState == initialState) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else if (transitionDirection == SwipeDirection.LEFT) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "home-tab-content",
            ) { tab ->
                when (tab) {
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

    if (unavailableThreadNotice) {
        AlertDialog(
            onDismissRequest = { unavailableThreadNotice = false },
            title = { Text("Conversación no disponible") },
            text = { Text("El mensaje ya no está disponible en Spam Blocker.") },
            confirmButton = {
                Button(onClick = { unavailableThreadNotice = false }) {
                    Text("Cerrar")
                }
            },
        )
    }
}

private fun Modifier.swipeBetweenHomeTabs(
    selectedTab: HomeTab,
    onSwipe: (SwipeDirection) -> Unit,
): Modifier {
    return pointerInput(selectedTab) {
        val threshold = 80.dp.toPx()
        awaitEachGesture {
            awaitPointerEvent(PointerEventPass.Initial)
            var dragDistance = Offset.Zero
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: break
                dragDistance += change.positionChange()
                if (change.changedToUp()) {
                    if (kotlin.math.abs(dragDistance.x) >= threshold &&
                        kotlin.math.abs(dragDistance.x) > kotlin.math.abs(dragDistance.y) * 1.5f
                    ) {
                        onSwipe(
                            if (dragDistance.x < 0f) {
                                SwipeDirection.LEFT
                            } else {
                                SwipeDirection.RIGHT
                            },
                        )
                    }
                    break
                }
            }
        }
    }
}
