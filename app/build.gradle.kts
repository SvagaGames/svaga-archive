import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Permanent signing key: created once by setup-signing.sh and kept outside the project (so updates install over each other).
val keyDir: String = System.getenv("SVAGA_KEYS")
    ?: listOf("/root/svaga/svaga1/svaga-keys", "/root/svaga-keys").firstOrNull { File("$it/keystore.properties").exists() }
    ?: "/root/svaga/svaga1/svaga-keys"
val keyProps = Properties().apply {
    val f = File("$keyDir/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
// The folder may have been moved: if the stored path is gone, look for the same file next to keystore.properties.
val keyFile: File? = keyProps.getProperty("storeFile")?.let { p ->
    val f = File(p)
    if (f.exists()) f else File(keyDir, f.name).takeIf { it.exists() }
}
val hasKey = keyFile != null

android {
    namespace = "com.svaga.archive"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.svaga.archive"
        minSdk = 26
        targetSdk = 36
        versionCode = 37
        versionName = "1.0"
    }

    // One code base, four shops. Google Play does not allow an app to update itself, so the APK self-update lives only
    // in the GitHub build; Play and RuStore builds update through their own stores.
    flavorDimensions += "store"
    productFlavors {
        create("github") { dimension = "store"; buildConfigField("String", "STORE", "\"github\"") }
        create("play") { dimension = "store"; buildConfigField("String", "STORE", "\"play\"") }
        create("rustore") { dimension = "store"; buildConfigField("String", "STORE", "\"rustore\"") }
        create("getapps") { dimension = "store"; buildConfigField("String", "STORE", "\"getapps\"") }
        create("appgallery") { dimension = "store"; buildConfigField("String", "STORE", "\"appgallery\"") }
    }

    if (hasKey) {
        signingConfigs {
            create("svaga") {
                storeFile = keyFile
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            if (hasKey) signingConfig = signingConfigs.getByName("svaga")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasKey) signingConfig = signingConfigs.getByName("svaga")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    "playImplementation"("com.google.android.play:app-update-ktx:2.1.0")
    "rustoreImplementation"("ru.rustore.sdk:appupdate:10.5.2")
}
