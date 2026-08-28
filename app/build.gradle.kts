import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
val releaseVersionName = providers.gradleProperty("releaseVersionName").orNull
val releaseVersionCodeText = providers.gradleProperty("releaseVersionCode").orNull
val releaseVersionCode = releaseVersionCodeText?.toIntOrNull()
    ?: if (releaseVersionCodeText == null) null else throw GradleException(
        "releaseVersionCode must be a valid integer, got: $releaseVersionCodeText",
    )

if ((releaseVersionName == null) != (releaseVersionCode == null)) {
    throw GradleException(
        "releaseVersionName and releaseVersionCode must be supplied together.",
    )
}

val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}
val releaseSigningValues = mapOf(
    "ANDROID_KEYSTORE_PATH" to providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull,
    "ANDROID_KEYSTORE_PASSWORD" to providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull,
    "ANDROID_KEY_ALIAS" to providers.environmentVariable("ANDROID_KEY_ALIAS").orNull,
    "ANDROID_KEY_PASSWORD" to providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull,
)
val missingReleaseSigningValues = releaseSigningValues.filterValues { it.isNullOrBlank() }.keys

if (releaseTaskRequested && missingReleaseSigningValues.isNotEmpty()) {
    throw GradleException(
        "Release signing is incomplete. Missing environment variables: " +
            missingReleaseSigningValues.sorted().joinToString(", "),
    )
}

android {
    namespace = "cn.loxx.expense"
    compileSdk = 37

    defaultConfig {
        applicationId = "cn.loxx.expense"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseVersionCode ?: 1
        versionName = releaseVersionName ?: "1.0"
    }

    val releaseSigningConfig = if (missingReleaseSigningValues.isEmpty()) {
        signingConfigs.create("release") {
            storeFile = file(requireNotNull(releaseSigningValues["ANDROID_KEYSTORE_PATH"]))
            storePassword = requireNotNull(releaseSigningValues["ANDROID_KEYSTORE_PASSWORD"])
            keyAlias = requireNotNull(releaseSigningValues["ANDROID_KEY_ALIAS"])
            keyPassword = requireNotNull(releaseSigningValues["ANDROID_KEY_PASSWORD"])
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = releaseSigningConfig
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Android already provides org.xmlpull.v1.* — exclude the xpp3 library globally
// to prevent R8 library/program class conflicts.
configurations.configureEach {
    exclude(group = "xpp3", module = "xpp3")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    implementation("androidx.room3:room3-runtime:3.0.1")
    ksp("androidx.room3:room3-compiler:3.0.1")
    implementation("androidx.sqlite:sqlite-bundled:2.7.0")

    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("com.itextpdf:kernel:7.2.6")
    implementation("com.itextpdf:io:7.2.6")
    implementation("com.itextpdf:layout:7.2.6")
    implementation("org.apache.poi:poi:5.5.1")
    implementation("org.apache.poi:poi-ooxml:5.5.1")
    implementation("com.fasterxml:aalto-xml:1.4.0")
    implementation("com.github.thegrizzlylabs:sardine-android:0.9")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
