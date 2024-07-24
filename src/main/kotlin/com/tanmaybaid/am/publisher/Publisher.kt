package com.tanmaybaid.am.publisher

interface Publisher {
    suspend fun publish(request: String, message: String)

    fun name() = this::class.simpleName!!.removeSuffix("Publisher")

    operator fun <T> List<T>.component1(): T? = getOrNull(0)
    operator fun <T> List<T>.component2(): T? = getOrNull(1)
}
