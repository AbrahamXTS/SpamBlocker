package net.abrahamespinosa.spam_blocker

import androidx.compose.runtime.Composable
import net.abrahamespinosa.spam_blocker.setup.SetupStateReader
import net.abrahamespinosa.spam_blocker.ui.home.HomeScreen as HomeContent

@Composable
fun HomeScreen(
    stateReader: SetupStateReader,
    onRequestRole: (String) -> Unit,
    onRequestContacts: () -> Unit,
) {
    HomeContent(
        stateReader = stateReader,
        onRequestRole = onRequestRole,
        onRequestContacts = onRequestContacts,
    )
}
