package com.tanmaybaid.am.model

import java.time.LocalDateTime

data class AvailableSlot(
    val locationId: Int,
    val startTimestamp: LocalDateTime,
    val endTimestamp: LocalDateTime,
    val active: Boolean,
    val duration: Int,
    val remoteInd: Boolean
)
