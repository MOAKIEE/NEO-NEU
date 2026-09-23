plugins { id("com.android.library") }
android { namespace = "edu.neu.campus.database"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies {
    api(project(":core:contract"))
    implementation("androidx.room:room-runtime:2.8.5")
    annotationProcessor("androidx.room:room-compiler:2.8.5")
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Aroom.schemaLocation=${projectDir}/schemas")
}
