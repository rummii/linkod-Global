package com.linkod.global.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.linkod.global.data.*
import com.linkod.global.ui.customer.CustomerBookingScreen
import com.linkod.global.ui.customer.CustomerJobsScreen
import com.linkod.global.ui.provider.ProviderWorkspaceScreen
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

    val scope = rememberCoroutineScope()

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var customerJobs by remember { mutableStateOf<List<JobResponse>>(emptyList()) }
    var providerOffers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var providerJobs by remember { mutableStateOf<List<JobResponse>>(emptyList()) }

    fun refreshData() {
        scope.launch {
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
    }

    LaunchedEffect(role) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linkod (${role.uppercase()})") },
                actions = {
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
}
