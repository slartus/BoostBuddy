import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
    id("kotlin-parcelize")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
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
            export(libs.decompose.extensions.experimental)
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
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            api(libs.decompose)
            api(libs.decompose.extensions.experimental)
            api(libs.decompose.compose)
            implementation(libs.composeImageLoader)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.core)
            implementation(libs.ktor.json)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.multiplatformSettings)
            implementation(libs.kodein.core)
            implementation(libs.kodein.conf)
            implementation(libs.collections.immutable)
            implementation(libs.napier)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activityCompose)
            implementation(libs.compose.uitooling)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)

            implementation(libs.exoplayer.core)
            implementation(libs.exoplayer.dash)
            implementation(libs.exoplayer.ui)
            implementation(libs.exoplayer.hls)
            implementation(libs.exoplayer.rtsp)
            implementation(libs.exoplayer.smoothstreaming)
            implementation(libs.android.splashscreen)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }
}

android {
    namespace = "ru.slartus.boostbuddy"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        applicationId = "ru.slartus.boostbuddy"
        versionCode = 57
        versionName = "1.0.7"
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/resources")
        resources.srcDirs("src/commonMain/resources")
    }
    compileOptions {
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

    signingConfigs {
        create("release") {
            val credentialsPath = "${rootDir.absolutePath}/keystore/keystore.properties"
            if (!File(credentialsPath).exists()) return@create
            val credentials = loadProperties(credentialsPath)

            keyAlias = credentials.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = credentials.getProperty("RELEASE_KEY_PASSWORD")
            storePassword = credentials.getProperty("RELEASE_STORE_PASSWORD")
            storeFile = File("${rootDir.absolutePath}/keystore/release.keystore")
        }
    }

    applicationVariants.all { variant ->
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "BoostBuddy_${variant.name}_${variant.versionName}.apk"
            }
        true
    }

    buildTypes {
        val release by getting {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}