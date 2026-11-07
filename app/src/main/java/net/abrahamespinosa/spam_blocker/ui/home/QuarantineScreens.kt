package net.abrahamespinosa.spam_blocker.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import net.abrahamespinosa.spam_blocker.data.QuarantineItemEntity

@Composable
fun QuarantineScreen(
    items: Flow<List<QuarantineItemEntity>>,
    selectedFilter: QuarantineFilter,
    onFilterChange: (QuarantineFilter) -> Unit,
    onOpenSms: (QuarantineItemEntity) -> Unit,
    onDiscard: (QuarantineItemEntity) -> Unit,
    onDeleteAll: (String) -> Unit,
    onRestoreSms: (QuarantineItemEntity) -> Unit,
) {
    val values by items.collectAsStateWithLifecycle(initialValue = emptyList())
    var deleteAllRequested by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val filterNoun = selectedFilter.noun
    val filteredItems = values
        .asSequence()
        .filter { it.type == selectedFilter.type }
        .sortedByDescending { it.receivedAt }
        .toList()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenIntro(
                title = "Cuarentena",
                description = "Estos eventos no generan notificaciones ni aparecen en las apps del sistema.",
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuarantineFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (filter == QuarantineFilter.MESSAGES) {
                                    Icons.AutoMirrored.Filled.Message
                                } else {
                                    Icons.Default.Phone
                                },
                                modifier = Modifier.size(16.dp),
                                contentDescription = null,
                            )
                        },
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            enabled = filteredItems.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Borrar $filterNoun") },
                                onClick = {
                                    menuExpanded = false
                                    deleteAllRequested = true
                                },
                            )
                        }
                    }
                }
            }
        }
        if (filteredItems.isEmpty()) {
            item {
                EmptyState(
                    title = if (values.isEmpty()) "Cuarentena vacía" else "No hay ${selectedFilter.emptyLabel}",
                    description = if (values.isEmpty()) {
                        "Los mensajes y llamadas bloqueados aparecerán aquí."
                    } else {
                        "No hay elementos de este tipo en cuarentena."
                    },
                )
            }
        }
        filteredItems.forEachIndexed { index, quarantineItem ->
            if (index == 0 || !sameCalendarDay(filteredItems[index - 1].receivedAt, quarantineItem.receivedAt)) {
                item(key = "quarantine-date-${quarantineItem.id}") {
                    DateSeparator(quarantineItem.receivedAt)
                }
            }
            item(key = "quarantine-item-${quarantineItem.id}") {
                var itemMenuExpanded by remember { mutableStateOf(false) }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (quarantineItem.type == "sms") {
                                Modifier.clickable { onOpenSms(quarantineItem) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    ListItem(
                        headlineContent = { Text(quarantineItem.address) },
                        supportingContent = {
                            Column {
                                Text(
                                    quarantineItem.body ?: "Llamada bloqueada",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatMessageTime(quarantineItem.receivedAt),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                        leadingContent = { QuarantineAvatar() },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { itemMenuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
                                }
                                DropdownMenu(
                                    expanded = itemMenuExpanded,
                                    onDismissRequest = { itemMenuExpanded = false },
                                ) {
                                    if (quarantineItem.type == "sms") {
                                        DropdownMenuItem(
                                            text = { Text("Restaurar SMS") },
                                            onClick = {
                                                itemMenuExpanded = false
                                                onRestoreSms(quarantineItem)
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Descartar") },
                                        onClick = {
                                            itemMenuExpanded = false
                                            onDiscard(quarantineItem)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (deleteAllRequested) {
        AlertDialog(
            onDismissRequest = { deleteAllRequested = false },
            title = { Text("Borrar $filterNoun") },
            text = { Text("¿Quieres eliminar ${selectedFilter.quantity} $filterNoun ${selectedFilter.adjective}?") },
            confirmButton = {
                Button(onClick = {
                    deleteAllRequested = false
                    onDeleteAll(selectedFilter.type)
                }) { Text("Borrar todo") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteAllRequested = false }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuarantineThreadScreen(
    address: String,
    items: Flow<List<QuarantineItemEntity>>,
    onBack: () -> Unit,
    onDeleteConversation: (String) -> Unit,
) {
    val values by items.collectAsStateWithLifecycle(initialValue = emptyList())
    var menuExpanded by remember { mutableStateOf(false) }
    var deleteConversationRequested by remember { mutableStateOf(false) }
    val threadItems = values
        .filter { it.type == "sms" && conversationKey(it.address) == conversationKey(address) }
        .sortedBy { it.receivedAt }
    val listState = rememberLazyListState()
    val lazyItemCount = threadItems.size + threadItems.indices.count { index ->
        index == 0 || !sameCalendarDay(threadItems[index - 1].receivedAt, threadItems[index].receivedAt)
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
            QuarantineAvatar()
            Text(
                address,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Borrar conversación") },
                        onClick = {
                            menuExpanded = false
                            deleteConversationRequested = true
                        },
                    )
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            threadItems.forEachIndexed { index, item ->
                if (index == 0 || !sameCalendarDay(threadItems[index - 1].receivedAt, item.receivedAt)) {
                    item(key = "quarantine-date-${item.id}") { DateSeparator(item.receivedAt) }
                }
                item(key = "quarantine-message-${item.id}") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomEnd = 18.dp,
                            bottomStart = 4.dp,
                        ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp,
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(item.body.orEmpty())
                            Text(
                                formatMessageTime(item.receivedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (deleteConversationRequested) {
        AlertDialog(
            onDismissRequest = { deleteConversationRequested = false },
            title = { Text("Borrar conversación") },
            text = { Text("¿Quieres eliminar todos los SMS de este remitente?") },
            confirmButton = {
                Button(onClick = {
                    deleteConversationRequested = false
                    onDeleteConversation(address)
                }) { Text("Borrar todo") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConversationRequested = false }) { Text("Cancelar") }
            },
        )
    }
}
