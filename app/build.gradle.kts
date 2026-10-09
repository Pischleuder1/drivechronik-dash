import com.google.protobuf.gradle.*
import java.io.File
import java.util.Properties
plugins {
    id("com.google.protobuf") version "0.10.0"
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}


val releaseSigningFile =
    File(
        System.getProperty("user.home"),
        ".config/drivechronik-dash/release-signing.properties"
    )

val releaseSigningProperties =
    Properties().apply {

        if (releaseSigningFile.isFile) {

            releaseSigningFile
                .inputStream()
                .use { input ->
                    load(input)
                }
        }
    }


android {
    namespace = "de.drivechronik.dash"
    compileSdk = 36
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "de.drivechronik.dash"
        minSdk = 31
        targetSdk = 36
        versionCode = 8
        versionName = "0.5.1"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningFile.exists()) {
                storeFile =
                    file(
                        releaseSigningProperties
                            .getProperty("storeFile")
                    )

                storePassword =
                    releaseSigningProperties
                        .getProperty("storePassword")

                keyAlias =
                    releaseSigningProperties
                        .getProperty("keyAlias")

                keyPassword =
                    releaseSigningProperties
                        .getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".routingtest"
        }
        release {
            signingConfig =
                signingConfigs.getByName("release")

            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("io.github.rallista:valhalla-mobile:0.6.3")
    implementation("io.github.rallista:valhalla-models:0.5.2")
    implementation("io.github.rallista:valhalla-models-config:0.5.2")
    implementation("com.stadiamaps:osrm-openapi:0.0.10")
    implementation("org.maplibre.gl:android-sdk-opengl:13.6.1")
    implementation("com.google.protobuf:protobuf-javalite:4.36.2")
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation("androidx.compose.material:material-icons-extended")

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation("org.robolectric:robolectric:4.14.1")
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}


protobuf {
    protoc {
        artifact =
            "com.google.protobuf:protoc:4.36.2"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
