plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "edu.neu.campus.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "edu.neu.campus.neoneu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:contract"))
    implementation(project(":data:repository"))
    implementation(project(":integration:auth-web"))
    implementation(project(":integration:ecode"))
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    // Compose's transitive Espresso version cannot inject input on Android 16.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
