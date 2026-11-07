package net.abrahamespinosa.spam_blocker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import net.abrahamespinosa.spam_blocker.data.KeywordEntity
import net.abrahamespinosa.spam_blocker.setup.SetupRequirement
import net.abrahamespinosa.spam_blocker.setup.SetupStateReader
import java.util.Locale

@Composable
fun KeywordsScreen(
    keywords: Flow<List<KeywordEntity>>,
    onAdd: (String) -> Unit,
    onDelete: (KeywordEntity) -> Unit,
) {
    val values by keywords.collectAsStateWithLifecycle(initialValue = emptyList())
    var word by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenIntro(
            title = "Palabras",
            description = "Los SMS nuevos que contengan una de estas palabras se moverán a Cuarentena. Se ignoran mayúsculas, pero no acentos.",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                modifier = Modifier.weight(1f),
                label = { Text("Nueva keyword") },
                singleLine = true,
            )
            FilledIconButton(
                onClick = { onAdd(word.trim()); word = "" },
                enabled = word.isNotBlank(),
            ) {
                Icon(Icons.Default.Check, contentDescription = "Agregar keyword")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(values, key = { it.id }) { keyword ->
                var menuExpanded by remember { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = {
                            Text(keyword.word.replaceFirstChar { it.titlecase(Locale.ROOT) })
                        },
                        leadingContent = { KeywordIcon() },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Más acciones")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Borrar palabra") },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete(keyword)
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
}

@Composable
fun StatusScreen(
    stateReader: SetupStateReader,
    onRequestRole: (String) -> Unit,
    onRequestContacts: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenIntro(
            title = "Estado",
            description = "El filtrado solo está activo cuando SMS, llamadas y contactos aparecen como activos.",
        )
        StatusButton(
            label = "App de SMS predeterminada",
            granted = stateReader.isGranted(SetupRequirement.SMS_ROLE),
            onClick = { onRequestRole(android.app.role.RoleManager.ROLE_SMS) },
        )
        StatusButton(
            label = "Filtrado de llamadas",
            granted = stateReader.isGranted(SetupRequirement.CALL_SCREENING_ROLE),
            onClick = { onRequestRole(android.app.role.RoleManager.ROLE_CALL_SCREENING) },
        )
        StatusButton(
            label = "Acceso a contactos",
            granted = stateReader.isGranted(SetupRequirement.READ_CONTACTS),
            onClick = onRequestContacts,
        )
    }
}

@Composable
private fun StatusButton(label: String, granted: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                Text(if (granted) "Activo" else "Falta este permiso; el filtrado no se garantiza")
            },
            trailingContent = {
                if (granted) {
                    Text("Listo", color = MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.material3.Button(onClick = onClick) { Text("Conceder") }
                }
            },
        )
    }
}
