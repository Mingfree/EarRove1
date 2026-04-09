package com.example.earrove.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earrove.utils.VolcengineArkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OcrUiState(
    val isRecognizing: Boolean = false,
    val recognitionResult: String = "",
    val isFlashlightOn: Boolean = false,
    val statusText: String = ""
)

class OcrViewModel : ViewModel() {

    private val arkService = VolcengineArkService()

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    suspend fun recognizeImage(base64: String): Result<String> =
        arkService.recognizeImage(base64)

    fun updateState(transform: (OcrUiState) -> OcrUiState) {
        _uiState.update(transform)
    }

    fun setStatusText(text: String) {
        _uiState.update { it.copy(statusText = text) }
    }

    override fun onCleared() {
        arkService.shutdown()
    }

    fun runAlbumRecognition(
        base64: String,
        statusRecognizing: String,
        statusDone: String,
        statusFailedRetry: String,
        statusFailed: String,
        formatFailure: (String) -> String,
        onSpeak: (String) -> Unit,
        onSpeakFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecognizing = true,
                    statusText = statusRecognizing,
                    recognitionResult = ""
                )
            }
            try {
                recognizeImage(base64)
                    .onSuccess { text ->
                        _uiState.update {
                            it.copy(
                                recognitionResult = text,
                                statusText = statusDone,
                                isRecognizing = false
                            )
                        }
                        onSpeak(text)
                    }
                    .onFailure { error ->
                        val msg = formatFailure(error.message ?: "")
                        _uiState.update {
                            it.copy(
                                recognitionResult = msg,
                                statusText = statusFailedRetry,
                                isRecognizing = false
                            )
                        }
                        onSpeakFailure(statusFailedRetry)
                    }
            } catch (e: Exception) {
                val msg = formatFailure(e.message ?: "")
                _uiState.update {
                    it.copy(
                        recognitionResult = msg,
                        statusText = statusFailedRetry,
                        isRecognizing = false
                    )
                }
                onSpeakFailure(statusFailed)
            }
        }
    }
}
