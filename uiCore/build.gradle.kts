plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
            api(libs.compose.material.icons.core)
        }
        jvmMain.dependencies {
            api(compose.desktop.common)
        }
        wasmJsMain.dependencies {
            api(libs.kotlinx.browser)
        }
    }
}

// Brand assets (logo) shared by both apps; the Res class must be public because uiCore is a library.
compose.resources {
    publicResClass = true
    packageOfResClass = "com.tapshop.ui.res"
    generateResClass = always
}
