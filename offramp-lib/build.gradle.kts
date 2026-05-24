plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("secant.kotlin-multiplatform-build-conventions")
    id("secant.dependency-conventions")

    id("org.jetbrains.kotlinx.kover")
    id("secant.kover-conventions")
}

kotlin {
    jvm()
    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serializable.json)
                api(projects.evmLib)
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        getByName("jvmMain") {
            dependencies {
                implementation(project.dependencies.enforcedPlatform(libs.ktor.bom))
                implementation(libs.ktor.core)
                implementation(libs.ktor.okhttp)
                implementation(libs.ktor.negotiation)
                implementation(libs.ktor.json)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.ktor.mock)
            }
        }
    }
}
