package net.abrahamespinosa.spam_blocker.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.abrahamespinosa.spam_blocker.data.InboxMessageEntity
import net.abrahamespinosa.spam_blocker.sms.SmsDeleteRepository
import net.abrahamespinosa.spam_blocker.sms.SmsSender

@Composable
fun InboxScreen(
    messages: Flow<List<InboxMessageEntity>>,
    onOpenThread: (String) -> Unit,
) {
    val values by messages.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingConversation by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val conversations = values
        .groupBy { conversationKey(it.address) }
        .values
        .map { conversation -> conversation.maxBy { it.receivedAt } }
        .sortedByDescending { it.receivedAt }

    MessageList(
        title = "Bandeja",
        description = "Tus conversaciones normales. Los SMS de Cuarentena no aparecen aquí.",
        emptyLabel = "No hay SMS",
        rows = conversations.map { it.address to it.body },
        onClick = onOpenThread,
        onLongClick = { pendingConversation = it },
    )

    pendingConversation?.let { address ->
        AlertDialog(
            onDismissRequest = { pendingConversation = null },
            title = { Text("Borrar conversación") },
            text = { Text("Se eliminarán todos los SMS de $address.") },
            confirmButton = {
                Button(onClick = {
                    pendingConversation = null
                    scope.launch {
                        runCatching {
                            SmsDeleteRepository(
                                context.contentResolver,
                                (context.applicationContext as net.abrahamespinosa.spam_blocker.SpamBlockerApplication)
                                    .database.inboxMessageDao(),
                            ).deleteConversation(address)
                        }.onFailure { deleteError = it.message ?: "No se pudo borrar la conversación." }
                    }
                }) { Text("Borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingConversation = null }) { Text("Cancelar") }
            },
        )
    }
    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text("Borrado local completado") },
            text = { Text(message) },
            confirmButton = { Button(onClick = { deleteError = null }) { Text("Cerrar") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(
    address: String,
    messages: Flow<List<InboxMessageEntity>>,
    onBack: () -> Unit,
) {
    val values by messages.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var body by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<InboxMessageEntity?>(null) }
    var deleteConversationRequested by remember { mutableStateOf(false) }
    var deleteWarning by remember { mutableStateOf<String?>(null) }
    var conversationDeleteError by remember { mutableStateOf<String?>(null) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val threadMessages = values
        .filter { conversationKey(it.address) == conversationKey(address) }
        .sortedBy { it.receivedAt }
    val listState = rememberLazyListState()
    val lazyItemCount = threadMessages.size + threadMessages.indices.count { index ->
        index == 0 || !sameCalendarDay(threadMessages[index - 1].receivedAt, threadMessages[index].receivedAt)
    }

    LaunchedEffect(lazyItemCount) {
        if (lazyItemCount > 0) listState.scrollToItem(lazyItemCount - 1)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            SenderAvatar(address, size = 40.dp)
            Text(
                address,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            ConversationMenu(onDelete = { deleteConversationRequested = true })
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            threadMessages.forEachIndexed { index, message ->
                if (index == 0 || !sameCalendarDay(threadMessages[index - 1].receivedAt, message.receivedAt)) {
                    item(key = "date-${message.id}") { DateSeparator(message.receivedAt) }
                }
                item(key = "message-${message.id}") {
                    Row(Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { pendingDelete = message },
                                ),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomEnd = 18.dp,
                                bottomStart = 4.dp,
                            ),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp,
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(message.body)
                                Text(
                                    formatMessageTime(message.receivedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensaje") },
                minLines = 1,
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    sendError = runCatching { SmsSender(context).send(address, body) }
                        .exceptionOrNull()?.message
                    if (sendError == null) body = ""
                },
                enabled = body.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }

    pendingDelete?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Borrar SMS") },
            text = { Text("¿Quieres eliminar este SMS?") },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    scope.launch {
                        runCatching {
                            SmsDeleteRepository(
                                context.contentResolver,
                                (context.applicationContext as net.abrahamespinosa.spam_blocker.SpamBlockerApplication)
                                    .database.inboxMessageDao(),
                            ).delete(message)
                        }.onSuccess { result ->
                            if (!result.providerConfirmed) {
                                deleteWarning = "Se quitó de Spam Blocker, pero el proveedor del sistema no confirmó el borrado."
                            }
                        }.onFailure {
                            deleteWarning = "Se quitó de Spam Blocker, pero no se pudo comprobar el provider."
                        }
                    }
                }) { Text("Borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }
    deleteWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { deleteWarning = null },
            title = { Text("Borrado local completado") },
            text = { Text(warning) },
            confirmButton = { Button(onClick = { deleteWarning = null }) { Text("Cerrar") } },
        )
    }
    sendError?.let { error ->
        AlertDialog(
            onDismissRequest = { sendError = null },
            title = { Text("No se pudo enviar") },
            text = { Text(error.ifBlank { "El sistema no aceptó el envío del SMS." }) },
            confirmButton = { Button(onClick = { sendError = null }) { Text("Cerrar") } },
        )
    }
    if (deleteConversationRequested) {
        AlertDialog(
            onDismissRequest = { deleteConversationRequested = false },
            title = { Text("Borrar conversación") },
            text = { Text("Se eliminarán todos los SMS de este remitente.") },
            confirmButton = {
                Button(onClick = {
                    deleteConversationRequested = false
                    scope.launch {
                        runCatching {
                            SmsDeleteRepository(
                                context.contentResolver,
                                (context.applicationContext as net.abrahamespinosa.spam_blocker.SpamBlockerApplication)
                                    .database.inboxMessageDao(),
                            ).deleteConversation(threadMessages)
                        }.onSuccess { result ->
                            if (result.failedCount > 0) {
                                conversationDeleteError =
                                    "${result.deletedCount} SMS eliminados; ${result.failedCount} no se pudieron confirmar."
                            } else {
                                onBack()
                            }
                        }.onFailure {
                            conversationDeleteError = "No se pudo borrar la conversación."
                        }
                    }
                }) { Text("Borrar todo") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConversationRequested = false }) { Text("Cancelar") }
            },
        )
    }
    conversationDeleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { conversationDeleteError = null },
            title = { Text("Borrado incompleto") },
            text = { Text(message) },
            confirmButton = { Button(onClick = { conversationDeleteError = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun ConversationMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Borrar conversación") },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}
