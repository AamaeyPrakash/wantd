import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        outputModuleName.set("buyerApp")
        browser {
            commonWebpackConfig {
                outputFileName = "buyerApp.js"
                // Dev server on 3000 so it never collides with the Ktor API on 8080 (see defaultApiBaseUrl()).
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply { port = 3000 }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":uiCore"))
        }
    }
}
