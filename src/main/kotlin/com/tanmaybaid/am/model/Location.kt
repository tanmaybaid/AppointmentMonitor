package com.tanmaybaid.am.model

data class Location(
    val id: Int,
    val name: String,
    val shortName: String,
) {
    val simpleName = "${shortName.trim().ifBlank { name.trim() }} ($id)"
}
