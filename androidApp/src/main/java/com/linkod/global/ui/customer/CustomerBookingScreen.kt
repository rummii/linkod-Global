package com.linkod.global.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linkod.global.data.*
import com.linkod.global.ui.components.AddressAutocomplete
import com.linkod.global.ui.components.LocationMapView
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingScreen(
    categories: List<Category>,
    onBookingSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedPlace by remember {
        mutableStateOf<DemoPlace?>(PlaceCatalogue.samplePlaces.first())
    }
    var customAddressInput by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(categories.firstOrNull()) }
    var unitDetails by remember { mutableStateOf("") }
    var accessNotes by remember { mutableStateOf("") }
    var bookingStatus by remember { mutableStateOf("") }
    var isBooking by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.first()
        }
    }

    val displayAddress = remember(customAddressInput, selectedPlace) {
        customAddressInput?.takeIf { it.isNotBlank() }
            ?: selectedPlace?.let { "${it.name}, ${it.address}" }
            ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Book a Service", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        AddressAutocomplete(
            selectedPlace = selectedPlace,
            onPlaceSelected = { place ->
                selectedPlace = place
                customAddressInput = null
            },
            onCustomAddressEntered = { typedText ->
                customAddressInput = typedText
                // Match typed text to known place if available
                val matched = PlaceCatalogue.samplePlaces.firstOrNull {
                    it.name.equals(typedText.trim(), ignoreCase = true) ||
                            "${it.name}, ${it.address}".equals(typedText.trim(), ignoreCase = true)
                }
                if (matched != null) {
                    selectedPlace = matched
                } else if (typedText.isNotBlank()) {
                    selectedPlace = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        selectedPlace?.let { place ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Selected Location: ${place.name}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    LocationMapView(
                        lat = place.lat,
                        lng = place.lng,
                        title = place.name
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = unitDetails,
            onValueChange = { unitDetails = it },
            label = { Text("Unit / Apartment / Building details (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = accessNotes,
            onValueChange = { accessNotes = it },
            label = { Text("Access Notes / Instructions (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Service Category:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory?.id == cat.id,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text("${cat.name} (${cat.currency} ${cat.base_price_minor / 100.0})")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (bookingStatus.isNotEmpty()) {
            Text(
                text = bookingStatus,
                color = if (bookingStatus.contains("failed", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val cat = selectedCategory
                val place = selectedPlace
                val finalAddr = displayAddress
                if (cat != null && finalAddr.isNotBlank()) {
                    isBooking = true
                    bookingStatus = "Submitting request..."
                    scope.launch {
                        try {
                            val req = JobCreateRequest(
                                category_id = cat.id,
                                address = finalAddr,
                                lat = place?.lat ?: 14.5995,
                                lng = place?.lng ?: 120.9842,
                                access_notes = accessNotes.ifBlank { null },
                                unit_details = unitDetails.ifBlank { null }
                            )
                            ApiService.instance.createJob(
                                auth = "Bearer demo-customer",
                                idempotencyKey = UUID.randomUUID().toString(),
                                request = req
                            )
                            bookingStatus = "Job created successfully!"
                            unitDetails = ""
                            accessNotes = ""
                            onBookingSuccess()
                        } catch (e: Exception) {
                            bookingStatus = "Booking failed: ${e.localizedMessage}"
                        } finally {
                            isBooking = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = selectedCategory != null && displayAddress.isNotBlank() && !isBooking
        ) {
            if (isBooking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Confirm & Book Service")
            }
        }
    }
}
