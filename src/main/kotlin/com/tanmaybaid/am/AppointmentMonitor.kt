package com.tanmaybaid.am

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class AppointmentMonitor : NoOpCliktCommand()

fun main(args: Array<String>) = AppointmentMonitor()
    .subcommands(TtpMonitor())
    .main(args)
