package com.example.jhqdriverapp

data class Site(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: String, // Mock coordinates
    val type: String // "yard", "quarry", "construction_site"
) {
    companion object {
        fun getMockSites(): List<Site> {
            return listOf(
                Site("001", "JHQ Main Yard", "Jalan Perindustrian 2", "3.1500° N, 101.7000° E", "yard"),
                Site("002", "KL Construction Site", "Jalan Tun Razak", "3.1390° N, 101.6869° E", "construction_site"),
                Site("003", "Shah Alam Quarry", "Persiaran Sultan", "3.0731° N, 101.5184° E", "quarry"),
                Site("004", "PJ Development Site", "Jalan Utara", "3.1073° N, 101.6067° E", "construction_site")
            )
        }
    }
}