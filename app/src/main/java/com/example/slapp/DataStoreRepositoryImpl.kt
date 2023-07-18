package com.example.slapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreRepositoryImpl @Inject constructor(
    private val context: Context
) : DataStoreRepository {

    override suspend fun save(key: String, value: Boolean) {
        val dataStoreKey = booleanPreferencesKey(key)
        context.dataStore.edit {settings ->
            settings[dataStoreKey] = value
        }
    }

    override suspend fun read(key: String): Boolean? {
        //context.dataStore.edit{it.clear()}
        val dataStoreKey = booleanPreferencesKey(key)
        val preferences = context.dataStore.data.first()
        return preferences[dataStoreKey]
        //return true
    }

    override suspend fun getAppNames(): Set<Preferences.Key<*>>? {
        val keys = context.dataStore.data
            .map {
                it.asMap().keys
            }
        return keys.firstOrNull()
    }
}