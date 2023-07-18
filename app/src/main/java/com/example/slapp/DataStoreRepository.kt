package com.example.slapp

interface DataStoreRepository {
    suspend fun save(key: String, value: Boolean)
    suspend fun read(key: String): Boolean?
}