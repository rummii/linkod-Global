package com.linkod.global.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.linkod.global.data.DemoPlace
import com.linkod.global.data.PlaceCatalogue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressAutocomplete(
    selectedPlace: DemoPlace?,
    onPlaceSelected: (DemoPlace) -> Unit,
    onCustomAddressEntered: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember {
        mutableStateOf(selectedPlace?.let { "${it.name}, ${it.address}" } ?: "")
    }
    var expanded by remember { mutableStateOf(false) }

    val filteredPlaces = remember(query) {
        PlaceCatalogue.search(query)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredPlaces.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                expanded = true
                onCustomAddressEntered(newValue)
            },
            label = { Text("Service Address (Search or type)") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        expanded = true
                    }
                },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        if (filteredPlaces.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredPlaces.forEach { place ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = place.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            val formatted = "${place.name}, ${place.address}"
                            query = formatted
                            onPlaceSelected(place)
                            onCustomAddressEntered(formatted)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
