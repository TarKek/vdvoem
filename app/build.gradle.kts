import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Один ключ подписи для CI и локальных сборок: иначе Android отказывается обновлять
// приложение поверх версии с другим ключом («Приложение не обновлено»).
// Локально — keystore.properties в корне (в git не попадает), в CI — переменные окружения.
val signingProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(property: String, env: String): String? =
    System.getenv(env) ?: signingProperties.getProperty(property)

val storeFilePath = signingValue("storeFile", "VDVOEM_STORE_FILE")

android {
    namespace = "app.vdvoem"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.vdvoem"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "0.10.0"
    }

    signingConfigs {
        if (storeFilePath != null) {
            create("vdvoem") {
                storeFile = file(storeFilePath)
                storePassword = signingValue("storePassword", "VDVOEM_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "VDVOEM_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "VDVOEM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            if (storeFilePath != null) signingConfig = signingConfigs.getByName("vdvoem")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (storeFilePath != null) signingConfig = signingConfigs.getByName("vdvoem")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("io.insert-koin:koin-androidx-compose:4.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Системный виджет на главном экране (Glance, CYCLE_SPEC §7).
    implementation("androidx.glance:glance-appwidget:1.1.1")

    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:sync"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:wishes"))
    implementation(project(":feature:gifts"))
    implementation(project(":feature:store"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:quizzes"))
    implementation(project(":feature:timetable"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:cycle"))

    debugImplementation("androidx.compose.ui:ui-tooling")

    // JVM-тесты чистой логики: только тестовая конфигурация.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
    // org.json в JVM-тестах не заглушка из android.jar, а рабочая реализация.
    testImplementation("org.json:json:20240303")
}
