plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("secant.kotlin-multiplatform-build-conventions")

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
                implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
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
                implementation("io.ktor:ktor-client-mock")
            }
        }
    }
}
