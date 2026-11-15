package com.linkod.global.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linkod.global.data.*
import com.linkod.global.ui.customer.CustomerBookingScreen
import com.linkod.global.ui.customer.CustomerJobsScreen
import com.linkod.global.ui.provider.ProviderWorkspaceScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var role by remember { mutableStateOf("customer") } // "customer" or "provider"
    var customerTab by remember { mutableStateOf(0) } // 0: Book, 1: My Jobs
    var showSettingsDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var customerJobs by remember { mutableStateOf<List<JobResponse>>(emptyList()) }
    var providerOffers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var providerJobs by remember { mutableStateOf<List<JobResponse>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun fetchCurrentData() {
        try {
            categories = ApiService.instance.getCategories()
            if (role == "customer") {
                customerJobs = ApiService.instance.getJobs("Bearer demo-customer")
            } else {
                providerOffers = ApiService.instance.getOffers("Bearer demo-provider")
                providerJobs = ApiService.instance.getJobs("Bearer demo-provider")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshData() {
        scope.launch {
            isRefreshing = true
            fetchCurrentData()
            isRefreshing = false
        }
    }

    // Initial load + periodic polling every 10 seconds
    LaunchedEffect(role) {
        fetchCurrentData()
        while (isActive) {
            delay(10000)
            fetchCurrentData()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Linkod (${role.uppercase()})") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Text("⚙️")
                    }
                    IconButton(onClick = { refreshData() }) {
                        Text(if (isRefreshing) "⏳" else "🔄")
                    }
                    TextButton(onClick = {
                        role = if (role == "customer") "provider" else "customer"
                    }) {
                        Text("Switch to ${if (role == "customer") "Provider" else "Customer"}")
                    }
                }
            )
        },
        bottomBar = {
            if (role == "customer") {
                NavigationBar {
                    NavigationBarItem(
                        selected = customerTab == 0,
                        onClick = { customerTab = 0 },
                        label = { Text("Book Service") },
                        icon = { Text("🛠️") }
                    )
                    NavigationBarItem(
                        selected = customerTab == 1,
                        onClick = { customerTab = 1 },
                        label = { Text("My Jobs (${customerJobs.size})") },
                        icon = { Text("📋") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (role == "customer") {
                when (customerTab) {
                    0 -> CustomerBookingScreen(
                        categories = categories,
                        onBookingSuccess = {
                            refreshData()
                            customerTab = 1
                        }
                    )
                    1 -> CustomerJobsScreen(
                        jobs = customerJobs,
                        onRefresh = { refreshData() }
                    )
                }
            } else {
                ProviderWorkspaceScreen(
                    offers = providerOffers,
                    activeJobs = providerJobs,
                    onRefresh = { refreshData() }
                )
            }
        }
    }

    if (showSettingsDialog) {
        ServerSettingsDialog(
            currentUrl = ApiService.baseUrl,
            onDismiss = { showSettingsDialog = false },
            onSave = { newUrl ->
                ApiService.updateBaseUrl(newUrl)
                showSettingsDialog = false
                refreshData()
                scope.launch {
                    snackbarHostState.showSnackbar("Server URL set to: $newUrl")
                }
            }
        )
    }
}

@Composable
fun ServerSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf(currentUrl) }
    var testStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set the backend host URL (e.g. your PC's IP on Wi-Fi or 10.0.2.2 for Android Emulator):",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Backend URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { urlInput = "http://10.0.2.2:3000/" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Emulator", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { urlInput = "http://localhost:3000/" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Localhost", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (testStatus.isNotEmpty()) {
                    Text(
                        testStatus,
                        color = if (testStatus.contains("Connected", ignoreCase = true))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        isTesting = true
                        testStatus = "Testing connection..."
                        scope.launch {
                            try {
                                val tempApi = ApiService.create(urlInput)
                                val res = tempApi.checkHealth()
                                testStatus = "Connected! Service: ${res["service"] ?: "ok"}"
                            } catch (e: Exception) {
                                testStatus = "Connection failed: ${e.localizedMessage}"
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isTesting && urlInput.isNotBlank()
                ) {
                    Text(if (isTesting) "Testing..." else "Test Connection")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(urlInput.trim()) },
                enabled = urlInput.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
