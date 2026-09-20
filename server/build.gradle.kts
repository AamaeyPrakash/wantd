plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

application {
    mainClass.set("com.tapshop.server.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.koog.agents)

    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)

    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
}

tasks.test {
    // Tests inject API fixtures or exercise missing-key errors; they never call the paid API.
    environment("OPENAI_API_KEY", "")
    environment("PUBLIC_BASE_URL", "https://wantd-demo.example.test")
}

tasks.named<JavaExec>("run") {
    // Serve the built web apps from the repo so one command runs the whole demo.
    workingDir = rootProject.projectDir
    standardInput = System.`in`
}
