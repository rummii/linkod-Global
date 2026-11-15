package com.linkod.global.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {
    @GET("health")
    suspend fun checkHealth(): Map<String, Any>

    @GET("api/categories")
    suspend fun getCategories(): List<Category>

    @GET("api/jobs")
    suspend fun getJobs(
        @Header("Authorization") auth: String
    ): List<JobResponse>

    @POST("api/jobs")
    suspend fun createJob(
        @Header("Authorization") auth: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: JobCreateRequest
    ): JobResponse

    @PATCH("api/jobs/{id}")
    suspend fun updateJobStatus(
        @Header("Authorization") auth: String,
        @Path("id") jobId: String,
        @Body request: Map<String, String>
    ): JobResponse

    @GET("api/providers/me/offers")
    suspend fun getOffers(
        @Header("Authorization") auth: String
    ): List<Offer>

    @PATCH("api/providers/me/location")
    suspend fun updateLocation(
        @Header("Authorization") auth: String,
        @Body request: LocationRequest
    ): Map<String, Any>

    @POST("api/offers/{id}/accept")
    suspend fun acceptOffer(
        @Header("Authorization") auth: String,
        @Path("id") offerId: String
    ): JobResponse

    @POST("api/offers/{id}/reject")
    suspend fun rejectOffer(
        @Header("Authorization") auth: String,
        @Path("id") offerId: String
    ): Map<String, Any>

    companion object {
        var baseUrl: String = "http://10.0.2.2:3000/"
            private set

        private var _instance: ApiService? = null

        val instance: ApiService
            get() {
                if (_instance == null) {
                    _instance = create(baseUrl)
                }
                return _instance!!
            }

        fun updateBaseUrl(newUrl: String) {
            val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
            baseUrl = formatted
            _instance = create(formatted)
        }

        fun create(url: String = baseUrl): ApiService {
            return Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
