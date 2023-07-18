package com.example.slapp

import androidx.datastore.preferences.core.Preferences

interface DataStoreRepository {
    suspend fun save(key: String, value: Boolean)
    suspend fun read(key: String): Boolean?
    suspend fun getAppNames(): Set<Preferences.Key<*>>?
}