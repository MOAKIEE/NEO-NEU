plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "edu.neu.campus.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    api(project(":core:contract"))
    api("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Miuix overlays use NavigationBackHandler, whose dispatcher is provided by
    // ComponentActivity starting with AndroidX Activity 1.12.
    api("androidx.activity:activity-compose:1.13.0")
    api("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    api("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    api("androidx.compose.ui:ui:1.7.8")
    api("androidx.compose.foundation:foundation:1.7.8")
    api("androidx.compose.foundation:foundation-layout:1.7.8")
    api("androidx.compose.runtime:runtime:1.7.8")
    api("androidx.compose.material:material-icons-core:1.7.8")
}
