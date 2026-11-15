package com.linkod.global.data

data class DemoPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

object PlaceCatalogue {
    val samplePlaces = listOf(
        DemoPlace("Rizal Park", "Ermita, Manila", 14.5826, 120.9787),
        DemoPlace("SM City Manila", "Natividad Almeda-Lopez Street, Ermita, Manila", 14.5897, 120.9839),
        DemoPlace("Manila City Hall", "Padre Burgos Avenue, Ermita, Manila", 14.5896, 120.9816),
        DemoPlace("Robinsons Place Manila", "Pedro Gil Street, Ermita, Manila", 14.5764, 120.9848),
        DemoPlace("University of Santo Tomas", "España Boulevard, Sampaloc, Manila", 14.6098, 120.9894),
        DemoPlace("Fort Santiago", "Intramuros, Manila", 14.5943, 120.9707),
        DemoPlace("SM Mall of Asia", "Seaside Boulevard, Pasay, Metro Manila", 14.5351, 120.9823),
        DemoPlace("Greenbelt", "Ayala Center, Makati, Metro Manila", 14.5513, 121.0205)
    )

    fun search(query: String): List<DemoPlace> {
        if (query.isBlank()) return samplePlaces
        return samplePlaces.filter {
            it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
        }
    }
}
