plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                moduleName = "sharedactor"
            }
        }
    }
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                moduleName = "sharedactor"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "sharedactor"
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
                api(libs.snftr.searchLib)
                api(libs.snftr.snftrDb)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain {
            dependencies{
                dependsOn(commonMain)
                implementation(libs.snftr.snftrDb)
            }
        }
        val iosSimulatorArm64Main by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        iosMain {
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies{
                dependsOn(commonMain)
            }
        }
        jvmMain {
            dependencies{
                dependsOn(commonMain)
            }
        }
    }
}

android {
    namespace = "com.exoteric.sharedactor"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

val org.jetbrains.kotlin.konan.target.KonanTarget.archVariant: String
    get() = if (this is org.jetbrains.kotlin.konan.target.KonanTarget.IOS_X64
        || this is org.jetbrains.kotlin.konan.target.KonanTarget.IOS_SIMULATOR_ARM64) {
        "ios-arm64_i386_x86_64-simulator"
    } else {
        "ios-arm64_armv7"
    }

val v = "1.2.4"
group = "com.exoteric"
version = v

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["kotlin"])
            groupId = "com.exoteric"
            artifactId = "sharedactor"
            version = v
        }
    }
}