plugins {
    application
    kotlin("jvm") version "2.0.0"
}

group = "com.tanmaybaid"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    // Ktor: https://ktor.io/docs/client-create-new-application.html#add-dependencies
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-apache5:2.3.12") // https://ktor.io/docs/client-engines.html
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12") // https://ktor.io/docs/client-serialization.html#add_content_negotiation_dependency
    implementation("io.ktor:ktor-serialization-jackson:2.3.12") // https://ktor.io/docs/client-serialization.html
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.tanmaybaid.am.AppointmentMonitorKt")
}

tasks {
    val fatJar = register<Jar>("appointment-monitor") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}

tasks.register<JavaExec>("execute") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
}

kotlin {
    jvmToolchain(21)
}
