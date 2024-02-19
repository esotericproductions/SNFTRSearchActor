plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    jvm()
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
//        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "SharedInteractor"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/commonMain/kotlin")
            dependencies {
                //put your multiplatform dependencies here
                implementation(libs.ktor.core)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.snftr.searchLib)
                implementation(libs.snftr.snftrDb)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        iosMain {
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            dependencies{
                dependsOn(commonMain)
                implementation(libs.snftr.snftrDb)
            }
        }
    }
}

android {
    namespace = "com.exoteric.searchinteractor"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

val v = "1.1.1"
group = "com.exoteric"
version = v

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["kotlin"])
            groupId = "com.exoteric"
            artifactId = "searchinteractor"
            version = v
        }
    }
}