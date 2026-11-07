package net.abrahamespinosa.spam_blocker.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.abrahamespinosa.spam_blocker.sms.SmsSender
import net.abrahamespinosa.spam_blocker.ui.theme.SpamBlockerTheme

class SmsRoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDestination = intent?.data?.schemeSpecificPart.orEmpty()
        setContent {
            SmsComposer(
                initialDestination = initialDestination,
                onSend = { destination, body ->
                    runCatching { SmsSender(this).send(destination, body) }
                        .exceptionOrNull()
                        ?.message
                        .also { error -> if (error == null) finish() }
                },
            )
        }
    }
}

@Composable
private fun SmsComposer(
    initialDestination: String,
    onSend: (String, String) -> String?,
) {
    var destination by remember { mutableStateOf(initialDestination) }
    var body by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf<String?>(null) }
    SpamBlockerTheme {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nuevo SMS", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destino") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Texto") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { sendError = onSend(destination.trim(), body) },
                enabled = destination.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enviar")
            }
            sendError?.let { error ->
                Text(error.ifBlank { "El sistema no aceptó el envío del SMS." })
            }
        }
    }
}