package com.example.wifiautomanager.domain.repository

/**
 * Repository interface for secure credential storage (passwords).
 * Decouples the domain layer from Android Keystore / EncryptedSharedPreferences.
 */
interface CredentialRepository {
    fun savePassword(networkId: Long, password: String)
    fun getPassword(networkId: Long): String?
    fun deletePassword(networkId: Long)
    fun hasPassword(networkId: Long): Boolean
    fun clearAll()
}
