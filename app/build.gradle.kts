import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.3.11"
}

// Release signing is optional at the Gradle level: keystore.properties and the keystore
// file it points to are both gitignored, so cloning this repo without them still builds
// debug and even an unsigned release — only :app:assembleRelease with a real signing
// identity needs this file. Never commit its contents or the keystore itself.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "com.gamilo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gamilo.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        // "-beta" suffix is deliberate: Phase 1/2/5 features are all built and tested, but this
        // hasn't been through real-world/multi-user usage yet. Bump to a plain "1.0.0" only once
        // Dez is confident enough in it to drop that qualifier.
        versionName = "1.0.0-beta.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Instrumented tests can't drive the biometric system prompt; this flag lets
            // the debug build bypass it. It must never leak into release (verify before
            // Stage 6) — compiled out there via the buildConfigField below.
            buildConfigField("boolean", "SKIP_BIOMETRIC_FOR_TESTS", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "SKIP_BIOMETRIC_FOR_TESTS", "false")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    androidResources {
        // Voice log's bundled Whisper model is loaded via a memory-mapped AssetFileDescriptor
        // (see ai/WhisperTranscriptionEngine.kt) — it must stay uncompressed in the APK for that to work.
        noCompress += "tflite"
    }

    sourceSets {
        getByName("androidTest") {
            // Lets a future MigrationTest load the Room-exported schema JSON via
            // MigrationTestHelper — see app/schemas/.
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.room:room-paging:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.paging:paging-runtime-ktx:3.5.1")
    implementation("androidx.paging:paging-compose:3.5.1")

    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.9.0")

    // Encrypted-at-rest Room DB: SQLCipher pages are ciphertext on disk, so a raw file copy
    // (backups, SAF export) is safe without any extra encryption step. The passphrase itself
    // is never stored in plaintext — see security/DbKeyManager.kt. Uses the actively-maintained
    // sqlcipher-android library (not the deprecated android-database-sqlcipher) since only this
    // one supports 16KB page-size-aligned native libraries, required for Play Store submission.
    implementation("net.zetetic:sqlcipher-android:4.18.0")
    implementation("androidx.sqlite:sqlite:2.7.0")

    // Bundled (not "unbundled") model — the recognizer ships inside the APK and runs fully
    // offline, no Google Play Services network fetch. Required for an air-gapped app.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Voice log: bundled Whisper tiny.en model (~42MB assets, see app/src/main/assets/models/),
    // driven with plain ByteBuffers. Depends directly on LiteRT (TensorFlow Lite's actively
    // maintained successor, same org.tensorflow.lite.* API surface) rather than the legacy
    // org.tensorflow:tensorflow-lite artifact, which only transitively pulled in an old,
    // non-16KB-page-aligned LiteRT build — Google Play requires 16KB page size support.
    // Deliberately NOT litert-support: that artifact's manifest collides with ML Kit's own
    // litert-support-api on the "org.tensorflow.lite.support" namespace and fails manifest merge.
    implementation("com.google.ai.edge.litert:litert:1.4.2")

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation("androidx.test:core:1.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
