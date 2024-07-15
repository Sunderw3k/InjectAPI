plugins {
    kotlin("jvm") version "2.0.0"
}

group = project.property("group") as String
version = project.property("version") as String
base.archivesName.set("InjectAPI")

val include: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

repositories {
    mavenCentral()
}

dependencies {
    val asmVersion = "9.7"
    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("org.ow2.asm:asm-commons:$asmVersion")
    implementation("org.ow2.asm:asm-tree:$asmVersion")

    implementation("com.google.guava:guava:33.2.1-jre")
}

tasks {
    compileJava {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "21"
            languageVersion = "2.0"
        }
    }
}
