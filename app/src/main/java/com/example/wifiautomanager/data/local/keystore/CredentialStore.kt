package com.example.wifiautomanager.data.local.keystore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.wifiautomanager.domain.repository.CredentialRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CredentialRepository {
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

    override fun savePassword(networkId: Long, password: String) {
        prefs.edit().putString("pwd_$networkId", password).apply()
    }

    override fun getPassword(networkId: Long): String? =
        prefs.getString("pwd_$networkId", null)

    override fun deletePassword(networkId: Long) {
        prefs.edit().remove("pwd_$networkId").apply()
    }

    override fun hasPassword(networkId: Long): Boolean =
        prefs.contains("pwd_$networkId")

    override fun clearAll() {
        prefs.edit().clear().apply()
    }
}
