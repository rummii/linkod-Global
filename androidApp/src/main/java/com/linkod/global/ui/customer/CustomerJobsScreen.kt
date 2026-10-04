package com.linkod.global.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linkod.global.data.ApiService
import com.linkod.global.data.JobResponse
import kotlinx.coroutines.launch

@Composable
fun CustomerJobsScreen(
    jobs: List<JobResponse>,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var actionStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Bookings", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onRefresh) {
                Text("🔄")
            }
        }

        if (actionStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(actionStatus, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No bookings found. Create a new service request above!")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(jobs) { job ->
                    JobItemCard(
                        job = job,
                        onCancel = {
                            scope.launch {
                                try {
                                    ApiService.instance.updateJobStatus(
                                        auth = "Bearer demo-customer",
                                        jobId = job.id.toString(),
                                        request = mapOf("status" to "cancelled")
                                    )
                                    actionStatus = "Job #${job.id} cancelled"
                                    onRefresh()
                                } catch (e: Exception) {
                                    actionStatus = "Failed to cancel: ${e.localizedMessage}"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JobItemCard(
    job: JobResponse,
    onCancel: () -> Unit
) {
    val statusColor = when (job.status.lowercase()) {
        "created", "open" -> MaterialTheme.colorScheme.primary
        "assigned", "en_route", "started" -> Color(0xFF2E7D32)
        "completed" -> Color(0xFF1565C0)
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Job #${job.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = job.status.uppercase(),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("📍 Address: ${job.address}", style = MaterialTheme.typography.bodyMedium)
            Text("💰 Price: ${job.currency} ${job.price_minor / 100.0}", style = MaterialTheme.typography.bodyMedium)
            Text("📅 Created: ${job.created_at}", style = MaterialTheme.typography.bodySmall)

            if (job.status.lowercase() == "created" || job.status.lowercase() == "open") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Request")
                }
            }
        }
    }
}
