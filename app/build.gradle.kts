import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.seprojectpart3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.seprojectpart3"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "GEMINI_MODEL",
            "\"${localProperties.getProperty("GEMINI_MODEL", "gemini-3.1-flash-lite-preview")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Unit Tests
    testImplementation("junit:junit:4.13.2")

    // Instrumentation Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")

    // Glide for loading proof images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CardView for proof submission items
    implementation("androidx.cardview:cardview:1.0.0")

    // Gmail SMTP for OTP
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    tasks.register<Javadoc>("generateRocketJavaDocs") {
        val localPropertiesFile = rootProject.file("local.properties")

        val sdkDir = if (localPropertiesFile.exists()) {
            localPropertiesFile.readText()
                .lines()
                .firstOrNull { it.startsWith("sdk.dir=") }
                ?.substringAfter("sdk.dir=")
                ?.replace("\\:", ":")
                ?.replace("\\\\", "\\")
        } else {
            null
        } ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: "C:\\Users\\nadsa\\AppData\\Local\\Android\\Sdk"

        val androidJar = file("$sdkDir\\platforms\\android-36\\android.jar")

        source = fileTree("src/main/java") {
            include("**/*.java")
        }

        classpath = files(androidJar) + configurations.getByName("debugCompileClasspath")

        destinationDir = file("$projectDir/javadocs_rocketevents")
        isFailOnError = false

        options.encoding = "UTF-8"
        (options as org.gradle.external.javadoc.StandardJavadocDocletOptions)
            .addStringOption("Xdoclint:none", "-quiet")
    }

}

