package com.example.slapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsDataViewModel @Inject constructor(
    private val repository: DataStoreRepository
) : ViewModel() {

    fun saveApp(appName: String, isSelected: Boolean) {
        viewModelScope.launch {
            repository.save(appName, isSelected)
        }
    }

    fun getApp(appName: String): Boolean? = runBlocking {
        repository.read(appName)
    }
}