plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Firebase push notifications need a google-services.json from your Firebase project.
// The build must not require it, so the plugin is applied only when the file exists.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.school.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.school.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 10.0.2.2 is the Android emulator's alias for the host machine's localhost,
        // where the Spring Boot backend runs during development.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${project.findProperty("apiBaseUrl") ?: "http://10.0.2.2:8080/api/v1/"}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.razorpay.checkout)
    implementation(libs.osmdroid.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)
    // Real org.json for JVM unit tests — the mockable android.jar only ships stubs,
    // and SubscriptionStatusInterceptor parses 403 bodies with JSONObject.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// JVM unit tests: two ViewModels (TransportViewModel's location poll, FeesViewModel's
// RazorpayResultBus collector) launch viewModelScope coroutines that loop/collect forever in
// production — ViewModel.onCleared() normally cancels them, but tests never call that, so each
// such test leaked a live coroutine for the rest of the JVM's life unless the test explicitly
// cancels viewModelScope (which the affected tests now do). Combined with MockK's per-mock
// ByteBuddy proxy generation, the accumulated garbage was enough to exhaust heap/metaspace and
// die with OutOfMemoryError (surfacing as native "java.lang.instrument ASSERTION FAILED" /
// JPLISAgent noise once the JVM couldn't allocate at all). Forking a JVM per test class made
// things worse here (each fork re-pays Kotlin/Compose/Hilt classloading + agent attach cost),
// so this sticks with one shared test JVM and just gives it more headroom.
tasks.withType<Test>().configureEach {
    maxHeapSize = "3g"
    jvmArgs("-XX:MaxMetaspaceSize=1g")
}
