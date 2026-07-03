// ScanViewModel.kt
package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.ScanResult
import com.pinza.hush.domain.repository.IScanResultRepository
import com.pinza.hush.domain.usecase.song.ScanSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanSongsUseCase: ScanSongsUseCase,
    private val scanResultRepository: IScanResultRepository
) : ViewModel() {

    private val _scanHistory = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanHistory: StateFlow<List<ScanResult>> = _scanHistory.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastScan = MutableStateFlow<ScanResult?>(null)
    val lastScan: StateFlow<ScanResult?> = _lastScan.asStateFlow()

    init {
        loadHistory()
        loadLastScan()
    }

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = 0
            _error.value = null

            try {
                // Simular progreso
                repeat(10) { step ->
                    _scanProgress.value = (step + 1) * 10
                    delay(200)
                }

                scanSongsUseCase()

                val result = ScanResult(
                    scanDate = System.currentTimeMillis(),
                    totalFound = 0,
                    status = "Completado"
                )
                scanResultRepository.insert(result)

                loadHistory()
                loadLastScan()
                _isScanning.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isScanning.value = false
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                scanResultRepository.history().collect { history ->
                    _scanHistory.value = history
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadLastScan() {
        viewModelScope.launch {
            try {
                val last = scanResultRepository.lastScan()
                _lastScan.value = last
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                scanResultRepository.clear()
                loadHistory()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}