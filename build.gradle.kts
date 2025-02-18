val ktorVersion = "3.1.0"
val log4jVersion = "2.24.3"

plugins {
    application
    kotlin("jvm") version "2.1.10"
}

group = "com.tanmaybaid"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Logging
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.5.0")
    implementation("org.slf4j:slf4j-simple:2+")

    // Ktor: https://ktor.io/docs/client-create-new-application.html#add-dependencies
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion") // https://ktor.io/docs/client-engines.html
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion") // https://ktor.io/docs/client-serialization.html#add_content_negotiation_dependency
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion") // https://ktor.io/docs/client-serialization.html

    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    // Jackson
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    runtimeOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-jul:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    // Needed for asynchronous logging by log4j2
    runtimeOnly("com.lmax:disruptor:3.4.4")
}

application {
    mainClass.set("com.tanmaybaid.am.AppointmentMonitorKt")
    applicationDefaultJvmArgs = listOf(
        "-XX:MaxRAMPercentage=90.0",
        "-XX:MaxGCPauseMillis=100",
        "-XX:+PerfDisableSharedMem",
        "-XX:+ExitOnOutOfMemoryError",
        "-XX:+ErrorFileToStderr",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "-Djava.net.preferIPv4Stack=true",
        "-Dsun.net.inetaddr.ttl=1",
        "-Dsun.net.inetaddr.negative.ttl=1",
        "-Dlog4j2.configurationFile=log4j2.xml",
        "-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager",
        "-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector",
    )
}

tasks {
    assemble {
        dependsOn(installDist)
    }
}
