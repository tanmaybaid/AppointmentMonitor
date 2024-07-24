package com.tanmaybaid.am.model

import java.time.LocalDateTime

data class SlotAvailability(
    val availableSlots: List<AvailableSlot>,
    val lastPublishedDate: LocalDateTime?
)

