package com.example.bwaibda25

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

// IMPORTANT: Replace this with YOUR deployed Cloud Function URL
// This URL should point to your 'get_ai_explanation' function.
private const val CLOUD_FUNCTION_URL = "YOUR_CLOUD_FUNCTION_TRIGGER_URL_HERE"

// Define states for the UI
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val explanation: String) : UiState()
    data class Error(val message: String) : UiState()
}

class AcronymViewModel : ViewModel() {

    private val _uiState = mutableStateOf<UiState>(UiState.Idle)
    val uiState: State<UiState> = _uiState

    private val client = OkHttpClient()

    fun getAcronymExplanation(acronym: String) {
        if (CLOUD_FUNCTION_URL == "YOUR_CLOUD_FUNCTION_TRIGGER_URL_HERE" || CLOUD_FUNCTION_URL.isBlank()) {
            _uiState.value = UiState.Error("Configuration Error: Cloud Function URL is not set.")
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val jsonRequestBody = JSONObject().apply { put("acronym", acronym) }
                val requestBody = jsonRequestBody.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(CLOUD_FUNCTION_URL)
                    .post(requestBody)
                    .build()

                // Execute network request on IO dispatcher
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBodyString = response.body?.string()

                if (response.isSuccessful && responseBodyString != null) {
                    _uiState.value = UiState.Success(responseBodyString)
                } else {
                    val errorMsg = "API Error: ${response.code} - ${response.message}\nBody: $responseBodyString"
                    _uiState.value = UiState.Error("Server Error: ${response.code}. Please try again.")
                }
                response.body?.close()

            } catch (e: JSONException) {
                _uiState.value = UiState.Error("Error creating request: ${e.localizedMessage}")
            } catch (e: IOException) {
                _uiState.value = UiState.Error("Network Error: ${e.localizedMessage}. Please check your connection.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
    }
}
