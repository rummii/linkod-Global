package com.linkod.global

import com.google.gson.Gson
import com.linkod.global.data.*
import org.junit.Assert.*
import org.junit.Test

class ModelsSerializationTest {

    private val gson = Gson()

    @Test
    fun category_deserialization() {
        val json = """{"id":1,"slug":"plumbing","name":"Plumbing","base_price_minor":50000,"currency":"PHP"}"""
        val category = gson.fromJson(json, Category::class.java)

        assertEquals(1, category.id)
        assertEquals("plumbing", category.slug)
        assertEquals("Plumbing", category.name)
        assertEquals(50000, category.base_price_minor)
        assertEquals("PHP", category.currency)
    }

    @Test
    fun jobResponse_deserialization() {
        val json = """{
            "id":"job-uuid-123",
            "customer_id":1,
            "provider_id":2,
            "category_id":1,
            "status":"assigned",
            "address":"Rizal Park, Ermita, Manila",
            "lat":14.5826,
            "lng":120.9787,
            "price_minor":50000,
            "currency":"PHP",
            "created_at":1700000000
        }"""
        val job = gson.fromJson(json, JobResponse::class.java)

        assertEquals("job-uuid-123", job.id)
        assertEquals("assigned", job.status)
        assertEquals(14.5826, job.lat!!, 0.0001)
        assertEquals(50000, job.price_minor)
    }

    @Test
    fun jobCreateRequest_serialization() {
        val request = JobCreateRequest(
            category_id = 1,
            address = "Rizal Park, Ermita, Manila",
            lat = 14.5826,
            lng = 120.9787,
            access_notes = "Ring bell",
            unit_details = "Unit 4B"
        )
        val json = gson.toJson(request)

        assertTrue(json.contains("\"category_id\":1"))
        assertTrue(json.contains("\"address\":\"Rizal Park, Ermita, Manila\""))
        assertTrue(json.contains("Unit 4B"))
    }
}
