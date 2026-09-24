plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "edu.neu.campus.authweb"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
}

dependencies {
    api(project(":core:contract"))
    implementation(project(":core:ui"))
    implementation(project(":data:session"))
    implementation(project(":data:network"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
}
