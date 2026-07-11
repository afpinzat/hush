package com.pinza.hush.ui.system

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionStatus>(PermissionStatus.Loading)
    val permissionState = _permissionState.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _permissionState.value = if (isGranted) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied
        }
    }
}

sealed class PermissionStatus {
    object Loading : PermissionStatus()
    object Granted : PermissionStatus()
    object Denied : PermissionStatus()
}