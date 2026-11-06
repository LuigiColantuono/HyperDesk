plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ukm.app.iconscustomizer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ukm.app.iconscustomizer"
        minSdk = 31
        targetSdk = 36
        versionCode = 6
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // CI signs releases with a fixed key from repository secrets, so a new version installs
    // over the old one. Without the key (local builds) the debug key is used.
    val releaseKeystore = System.getenv("HYPERDESK_KEYSTORE")
    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("HYPERDESK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HYPERDESK_KEY_ALIAS")
                keyPassword = System.getenv("HYPERDESK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    val preferenceVersion = "1.2.1"
    // Kotlin
    implementation("androidx.preference:preference-ktx:$preferenceVersion")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    compileOnly("io.github.libxposed:api:101.0.1")
    implementation("io.github.libxposed:service:101.0.0")
}