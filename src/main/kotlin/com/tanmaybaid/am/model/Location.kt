package com.tanmaybaid.am.model

data class Location(
    val id: Int,
    val name: String,
    val shortName: String,
    val locationType: String,
    val locationCode: String,
    val address: String,
    val addressAdditional: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val countryCode: String,
    val tzData: String,
    val temporary: Boolean,
    val inviteOnly: Boolean,
    val operational: Boolean,
    val services: List<LocationService>,
) {
    val simpleName = "${shortName.trim()} ($id)"
}
