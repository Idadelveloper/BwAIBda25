package com.example.bwaibda25 // Your package name

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
import java.util.concurrent.TimeUnit // Import TimeUnit for timeouts

// This should be the placeholder string you'd use if the URL wasn't set yet.
private const val PLACEHOLDER_URL = "YOUR_CLOUD_FUNCTION_TRIGGER_URL_HERE"

// IMPORTANT: Replace PLACEHOLDER_URL with YOUR deployed Cloud Function URL below.
// This URL should point to your 'get_ai_explanation' function.
// Example: "https://your-function-name-blahblah-uc.a.run.app"
private const val CLOUD_FUNCTION_URL = "CLOUD_RUN_FUNCTIONS" // User's provided URL

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

    // Create a single OkHttpClient instance with custom timeouts.
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Connection timeout
        .readTimeout(60, TimeUnit.SECONDS)    // Read timeout
        .writeTimeout(60, TimeUnit.SECONDS)   // Write timeout
        .build()

    fun getAcronymExplanation(acronym: String) {
        // Corrected check: Ensure the developer has replaced the placeholder or the URL is not blank.
        if (CLOUD_FUNCTION_URL == PLACEHOLDER_URL || CLOUD_FUNCTION_URL.isBlank()) {
            _uiState.value = UiState.Error("Configuration Error: Cloud Function URL is not set. Please update it in AcronymViewModel.kt.")
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val jsonRequestBody = JSONObject().apply { put("acronym", acronym) }
                val requestBodyString = jsonRequestBody.toString()
                val body = requestBodyString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(CLOUD_FUNCTION_URL)
                    .post(body)
                    .build()

                // Execute network request on IO dispatcher for network operations.
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                // It's crucial to read the response body only once.
                val responseBodyString = response.body?.string()

                if (response.isSuccessful && responseBodyString != null) {
                    _uiState.value = UiState.Success(responseBodyString)
                } else {
                    // Provide more context for errors.
                    val errorDetails = "Code: ${response.code}, Message: ${response.message}, Body: ${responseBodyString ?: "N/A"}"
                    _uiState.value = UiState.Error("Server Error: ${response.code}. Please try again. Details: $errorDetails")
                }
                // Ensure the response body is closed to free resources.
                response.body?.close()

            } catch (e: JSONException) {
                _uiState.value = UiState.Error("Error creating request (JSON): ${e.localizedMessage}")
            } catch (e: java.net.SocketTimeoutException) { // Specifically catch SocketTimeoutException
                _uiState.value = UiState.Error("Network Timeout: The request took too long to respond. Please try again. (${e.localizedMessage})")
            } catch (e: IOException) { // Catches other network errors (no connection, etc.)
                _uiState.value = UiState.Error("Network Error: ${e.localizedMessage}. Please check your connection.")
            } catch (e: Exception) { // Catch-all for other unexpected errors
                _uiState.value = UiState.Error("An unexpected error occurred: ${e.localizedMessage}")
            }
        }
    }
}
