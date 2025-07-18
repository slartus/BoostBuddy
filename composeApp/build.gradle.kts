plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "BoostBuddyShared"
            export(libs.decompose)
            export(libs.decompose.compose)
            export(libs.essently.lifecycle)
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            api(libs.decompose)
            api(libs.decompose.compose)
            implementation(libs.composeImageLoader)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.auth)
            implementation(libs.ktor.core)
            implementation(libs.ktor.json)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.settings.core)
            implementation(libs.settings.coroutines)
            implementation(libs.kodein.core)
            implementation(libs.kodein.conf)
            implementation(libs.collections.immutable)
            implementation(libs.napier)
            implementation(libs.qrose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activityCompose)
            implementation(libs.compose.uitooling)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.okhttp)

            implementation(libs.exoplayer.core)
            implementation(libs.exoplayer.dash)
            implementation(libs.exoplayer.ui)
            implementation(libs.exoplayer.hls)
            implementation(libs.exoplayer.rtsp)
            implementation(libs.exoplayer.smoothstreaming)
            implementation(libs.android.splashscreen)
        }

        iosMain.dependencies {
            implementation(libs.ktor.darwin)
        }

    }
}

android {
    namespace = "ru.slartus.boostbuddy"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        applicationId = "ru.slartus.boostbuddy"
        versionCode = 73
        versionName = "1.7.0"
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/resources")
        resources.srcDirs("src/commonMain/resources")
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}