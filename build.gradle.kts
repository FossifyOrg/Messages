plugins {
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.ksp).apply(false)
}

tasks.register<Delete>("clean") {
    delete {
        rootProject.layout.buildDirectory
    }
}
