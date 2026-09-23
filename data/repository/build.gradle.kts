plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.repository"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:contract")); implementation(project(":data:session")); implementation(project(":data:network")); implementation(project(":data:database")); implementation(project(":integration:academic")); implementation(project(":integration:portal")); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") }
