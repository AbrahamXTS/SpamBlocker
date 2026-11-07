package net.abrahamespinosa.spam_blocker.setup

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class SetupRequirement {
    SMS_ROLE,
    CALL_SCREENING_ROLE,
    READ_CONTACTS,
}

class SetupStateReader(private val context: Context) {
    fun isGranted(requirement: SetupRequirement): Boolean = when (requirement) {
        SetupRequirement.SMS_ROLE -> roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        SetupRequirement.CALL_SCREENING_ROLE ->
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        SetupRequirement.READ_CONTACTS ->
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun allGranted(): Boolean = SetupRequirement.entries.all(::isGranted)

    private val roleManager: RoleManager
        get() = context.getSystemService(RoleManager::class.java)
}