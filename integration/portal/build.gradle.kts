plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.portal"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:contract")); implementation(project(":data:network")); testImplementation("junit:junit:4.13.2"); testImplementation("org.json:json:20240303") }
