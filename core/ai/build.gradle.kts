plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.smartnotes.core.ai"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 26

        val localFile = rootProject.file("local.properties")
        val geminiApiKey = if (localFile.exists()) {
            localFile.readLines()
                .firstOrNull { it.trim().startsWith("GEMINI_API_KEY=") }
                ?.substringAfter("GEMINI_API_KEY=")
                ?.trim()
                .orEmpty()
        } else {
            ""
        }.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.generative.ai)

    implementation(libs.kotlinx.coroutines.android)
}
