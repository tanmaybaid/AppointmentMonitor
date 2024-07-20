package com.tanmaybaid.am.publisher

interface Publisher {
    suspend fun publish(request: String, message: String)

    fun name() = this::class.simpleName!!.removeSuffix("Publisher")
}
