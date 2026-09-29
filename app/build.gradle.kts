plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.leonardo.apptreino"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.leonardo.apptreino"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // Gera uma classe de binding para cada layout XML (substitui o findViewById).
        viewBinding = true
    }

    testOptions {
        // Permite chamar métodos do framework Android em testes unitários sem quebrar.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Base AndroidX + Material Design 3
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)

    // Listas e ciclo de vida (ViewModel + coroutines atreladas ao lifecycle)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Carregamento das miniaturas dos vídeos do YouTube
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}
