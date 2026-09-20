import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

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
        outputModuleName.set("merchantApp")
        browser {
            commonWebpackConfig {
                outputFileName = "merchantApp.js"
                // Dev server on 3001 so it never collides with the Ktor API on 8080 (see defaultApiBaseUrl()).
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply { port = 3001 }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":uiCore"))
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.tapshop.merchant.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "wantd. Merchant"
            packageVersion = "1.0.0"
        }
    }
}
