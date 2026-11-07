import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Release signing is configurable, never hard-coded.
 *
 * Locally:
 *   android/keystore.properties  (git-ignored) with storeFile / storePassword / keyAlias / keyPassword
 * In CI:
 *   XINGCLASS_KEYSTORE_PATH / XINGCLASS_KEYSTORE_PASSWORD / XINGCLASS_KEY_ALIAS / XINGCLASS_KEY_PASSWORD
 *
 * Packaging a release without a complete keystore fails: a debug-signed APK must never be
 * mistaken for a publishable artifact.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStorePath = signingValue("XINGCLASS_KEYSTORE_PATH", "storeFile")
val hasReleaseKeystore = !releaseStorePath.isNullOrBlank() &&
    file(releaseStorePath).isFile &&
    !signingValue("XINGCLASS_KEYSTORE_PASSWORD", "storePassword").isNullOrBlank() &&
    !signingValue("XINGCLASS_KEY_ALIAS", "keyAlias").isNullOrBlank() &&
    !signingValue("XINGCLASS_KEY_PASSWORD", "keyPassword").isNullOrBlank()

// Fail before a release artifact is packaged instead of silently emitting a debug-signed APK.
val releasePackagingTasks = setOf("assembleRelease", "bundleRelease", "packageRelease")
gradle.taskGraph.whenReady {
    val packagesRelease = allTasks.any { it.name in releasePackagingTasks }
    if (packagesRelease && !hasReleaseKeystore) {
        throw GradleException(
            "Release signing is required. Configure XINGCLASS_KEYSTORE_PATH, " +
                "XINGCLASS_KEYSTORE_PASSWORD, XINGCLASS_KEY_ALIAS and XINGCLASS_KEY_PASSWORD, " +
                "or put storeFile/storePassword/keyAlias/keyPassword in android/keystore.properties.",
        )
    }
}

android {
    namespace = "com.xingclass.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xingclass.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // XingClass ships only arm64-v8a. The app itself contains no native code today, so this
        // is a guard that keeps any future native dependency from silently adding other ABIs.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = signingValue("XINGCLASS_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("XINGCLASS_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("XINGCLASS_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        checkDependencies = false
        // This app ships a single Chinese UI. Translation checks are not meaningful yet.
        // arm64-only is an explicit product requirement; ChromeOS x86 support is out of scope.
        disable += setOf("MissingTranslation", "ExtraTranslation", "ChromeOsAbiSupport")
    }

    // AGP's public VariantOutput API has no filename property yet, so the stable (if deprecated)
    // applicationVariants hook is used to produce the agreed artifact name:
    // XingClass-arm64-v8a-debug.apk / XingClass-arm64-v8a-release.apk
    @Suppress("DEPRECATION")
    applicationVariants.all {
        val variantName = name
        outputs.all {
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                ?.outputFileName = "XingClass-arm64-v8a-$variantName.apk"
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
