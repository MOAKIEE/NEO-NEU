plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.network"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:contract")); implementation(project(":data:session")); api("com.squareup.okhttp3:okhttp:4.12.0"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"); testImplementation("junit:junit:4.13.2") }
