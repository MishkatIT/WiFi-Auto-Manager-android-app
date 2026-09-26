package com.example.wifiautomanager.data.local.keystore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "wifi_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassword(networkId: Long, password: String) {
        prefs.edit().putString("pwd_$networkId", password).apply()
    }

    fun getPassword(networkId: Long): String? =
        prefs.getString("pwd_$networkId", null)

    fun deletePassword(networkId: Long) {
        prefs.edit().remove("pwd_$networkId").apply()
    }

    fun hasPassword(networkId: Long): Boolean =
        prefs.contains("pwd_$networkId")

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
