package com.linkod.global

import com.linkod.global.data.PlaceCatalogue
import org.junit.Assert.*
import org.junit.Test

class PlaceCatalogueTest {

    @Test
    fun samplePlaces_isNotEmpty() {
        val places = PlaceCatalogue.samplePlaces
        assertTrue("Sample places should not be empty", places.isNotEmpty())
        assertEquals(8, places.size)
    }

    @Test
    fun search_returnsMatchingPlaces() {
        val results = PlaceCatalogue.search("Manila")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.name.contains("Manila", ignoreCase = true) || it.address.contains("Manila", ignoreCase = true) })
    }

    @Test
    fun search_emptyQuery_returnsAllPlaces() {
        val all = PlaceCatalogue.search("")
        assertEquals(PlaceCatalogue.samplePlaces.size, all.size)
    }

    @Test
    fun search_nonMatchingQuery_returnsEmptyList() {
        val results = PlaceCatalogue.search("NonExistentCityXYZ")
        assertTrue(results.isEmpty())
    }
}
