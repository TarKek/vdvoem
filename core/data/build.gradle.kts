plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.vdvoem.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
