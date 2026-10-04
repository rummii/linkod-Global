package com.linkod.global.ui.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linkod.global.data.ApiService
import com.linkod.global.data.JobResponse
import com.linkod.global.data.LocationRequest
import com.linkod.global.data.Offer
import kotlinx.coroutines.launch

@Composable
fun ProviderWorkspaceScreen(
    offers: List<Offer>,
    activeJobs: List<JobResponse>,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isOnline by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

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
            Text("Provider Workspace", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isOnline) "Online" else "Offline")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isOnline,
                    onCheckedChange = { online ->
                        isOnline = online
                        scope.launch {
                            try {
                                ApiService.instance.updateLocation(
                                    auth = "Bearer demo-provider",
                                    request = LocationRequest(
                                        lat = 14.5995,
                                        lng = 120.9842,
                                        active = online
                                    )
                                )
                                statusMessage = if (online) "Online & GPS updated" else "Provider offline"
                                onRefresh()
                            } catch (e: Exception) {
                                statusMessage = "Failed: ${e.localizedMessage}"
                            }
                        }
                    }
                )
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(statusMessage, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Incoming Offers", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (offers.isEmpty()) {
            Text(
                "No open offers at the moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(offers) { offer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Offer #${offer.id} for Job #${offer.job_id}", style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${offer.status}", style = MaterialTheme.typography.bodySmall)
                            Text("Expires: ${offer.expires_at}", style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                ApiService.instance.acceptOffer("Bearer demo-provider", offer.id.toString())
                                                statusMessage = "Offer #${offer.id} accepted!"
                                                onRefresh()
                                            } catch (e: Exception) {
                                                statusMessage = "Accept failed: ${e.localizedMessage}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Accept Offer")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                ApiService.instance.rejectOffer("Bearer demo-provider", offer.id.toString())
                                                statusMessage = "Offer #${offer.id} rejected"
                                                onRefresh()
                                            } catch (e: Exception) {
                                                statusMessage = "Reject failed: ${e.localizedMessage}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Assigned Active Jobs", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (activeJobs.isEmpty()) {
            Text(
                "No assigned jobs currently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(activeJobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Job #${job.id} - Status: ${job.status}", style = MaterialTheme.typography.titleSmall)
                            Text("Address: ${job.address}", style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (job.status.lowercase() == "assigned") {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    ApiService.instance.updateJobStatus(
                                                        auth = "Bearer demo-provider",
                                                        jobId = job.id.toString(),
                                                        request = mapOf("status" to "started")
                                                    )
                                                    statusMessage = "Service started for Job #${job.id}"
                                                    onRefresh()
                                                } catch (e: Exception) {
                                                    statusMessage = "Failed: ${e.localizedMessage}"
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Start Service")
                                    }
                                }

                                if (job.status.lowercase() == "started") {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    ApiService.instance.updateJobStatus(
                                                        auth = "Bearer demo-provider",
                                                        jobId = job.id.toString(),
                                                        request = mapOf("status" to "completed")
                                                    )
                                                    statusMessage = "Job #${job.id} marked as Completed!"
                                                    onRefresh()
                                                } catch (e: Exception) {
                                                    statusMessage = "Failed: ${e.localizedMessage}"
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Complete Service")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
