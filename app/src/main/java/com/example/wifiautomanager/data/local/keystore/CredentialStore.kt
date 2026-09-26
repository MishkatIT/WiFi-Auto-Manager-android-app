package com.example.wifiautomanager.data.local.keystore

// Encrypted CredentialStore backed by Android Keystore - implemented in Phase 2
interface CredentialStore {
    suspend fun savePassword(networkId: Long, password: String)
    suspend fun getPassword(networkId: Long): String?
    suspend fun deletePassword(networkId: Long)
}
