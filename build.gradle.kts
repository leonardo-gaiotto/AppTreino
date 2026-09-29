// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // O AGP 9 já traz Kotlin embutido; declarar o plugin aqui apenas define a VERSÃO do
    // compilador Kotlin, compatível com as bibliotecas AndroidX/Coil mais recentes.
    alias(libs.plugins.kotlin.android) apply false
}
