package com.example.bwaibda25

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    // Initialize ViewModel using activity-compose library
    private val viewModel: AcronymViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DevAcronymBusterTheme { // Replace with your app's theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevAcronymBusterScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevAcronymBusterScreen(viewModel: AcronymViewModel) {
    var acronymInput by remember { mutableStateOf("") }
    val uiState by viewModel.uiState // Observe the state from ViewModel
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build with AI Bamenda 2025") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dev Acronym Buster 💡",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = acronymInput,
                onValueChange = { acronymInput = it.uppercase() },
                label = { Text("Enter a tech acronym (e.g., API)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (acronymInput.isNotBlank()) {
                            if (isNetworkAvailable(context)) {
                                viewModel.getAcronymExplanation(acronymInput)
                            } else {
                                Toast.makeText(context, "No internet connection.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Acronym cannot be empty.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (acronymInput.isNotBlank()) {
                        if (isNetworkAvailable(context)) {
                            viewModel.getAcronymExplanation(acronymInput)
                        } else {
                            Toast.makeText(context, "No internet connection.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Acronym cannot be empty.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is UiState.Loading // Disable button when loading
            ) {
                Text("Explain Acronym", modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Handle UI state changes
            when (val state = uiState) {
                is UiState.Idle -> {
                    // Optionally show a placeholder message
                    Text("Enter an acronym above and click explain.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp))
                }
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is UiState.Success -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Allow card to take remaining space
                            .padding(top = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()) // Make explanation scrollable
                        ) {
                            Text(
                                text = state.explanation,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to check for network availability (can be moved to a utility file)
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

// Basic Theme (You'll likely have your own theme defined)
@Composable
fun DevAcronymBusterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme( // or darkColorScheme()
            primary = MaterialTheme.colorScheme.primary, // Example, customize as needed
            // ... other colors
        ),
        typography = Typography(), // Define your typography
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DevAcronymBusterTheme {
        // You can pass a mock ViewModel for preview if needed
        // For simplicity, this preview might not fully render dynamic states
//        DevAcronymBusterScreen(viewModel = AcronymViewModel())
    }
}
