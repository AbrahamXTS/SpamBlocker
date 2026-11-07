package net.abrahamespinosa.spam_blocker

import android.os.Bundle
import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.abrahamespinosa.spam_blocker.setup.SetupRequirement
import net.abrahamespinosa.spam_blocker.setup.SetupStateReader
import net.abrahamespinosa.spam_blocker.sms.SmsImportRepository
import net.abrahamespinosa.spam_blocker.ui.theme.SpamBlockerTheme

private enum class ImportState {
    IDLE,
    IMPORTING,
    COMPLETE,
    FAILED,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpamBlockerApp() }
    }
}

@Composable
private fun SpamBlockerApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableStateOf(0) }
    var importState by remember { mutableStateOf(ImportState.IDLE) }
    val stateReader = remember(context, refreshKey) { SetupStateReader(context) }
    val application = context.applicationContext as SpamBlockerApplication
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshKey++ }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshKey++ }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(stateReader.allGranted()) {
        if (stateReader.allGranted()) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            val repository = SmsImportRepository(
                contentResolver = context.contentResolver,
                inboxMessageDao = application.database.inboxMessageDao(),
                syncStateDao = application.database.syncStateDao(),
            )
            importState = ImportState.IMPORTING
            importState = runCatching {
                if (!repository.isInitialImportComplete()) {
                    repository.importExistingMessages()
                    repository.markInitialImportComplete()
                }
                ImportState.COMPLETE
            }.getOrElse { ImportState.FAILED }
        } else {
            importState = ImportState.IDLE
        }
    }

    SpamBlockerTheme {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (stateReader.allGranted() && importState == ImportState.COMPLETE) {
                HomeScreen(
                stateReader = stateReader,
                onRequestRole = { role ->
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    roleLauncher.launch(roleManager.createRequestRoleIntent(role))
                },
                onRequestContacts = {
                    contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                )
            } else if (!stateReader.allGranted()) {
                SetupScreen(
                stateReader = stateReader,
                onRequestRole = { role ->
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    roleLauncher.launch(roleManager.createRequestRoleIntent(role))
                },
                onRequestContacts = {
                    contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (importState == ImportState.FAILED) {
                            "No se pudieron importar los SMS. Vuelve a abrir la app para reintentar."
                        } else {
                            "Importando mensajes existentes..."
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    stateReader: SetupStateReader,
    onRequestRole: (String) -> Unit,
    onRequestContacts: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Configura Spam-blocker", style = MaterialTheme.typography.headlineMedium)
            Text("Completa estos permisos para activar el filtrado.")
        SetupButton(
            label = "App de SMS predeterminada",
            granted = stateReader.isGranted(SetupRequirement.SMS_ROLE),
            onClick = { onRequestRole(RoleManager.ROLE_SMS) },
        )
        SetupButton(
            label = "Filtrado de llamadas",
            granted = stateReader.isGranted(SetupRequirement.CALL_SCREENING_ROLE),
            onClick = { onRequestRole(RoleManager.ROLE_CALL_SCREENING) },
        )
        SetupButton(
            label = "Acceso a contactos",
            granted = stateReader.isGranted(SetupRequirement.READ_CONTACTS),
            onClick = onRequestContacts,
        )
    }
}

@Composable
private fun SetupButton(label: String, granted: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(label) },
            supportingContent = {
                Text(if (granted) "Activo" else "Necesario para activar el filtrado")
            },
            trailingContent = {
                Button(onClick = onClick, enabled = !granted) {
                    Text(if (granted) "Listo" else "Conceder")
                }
            },
        )
    }
}