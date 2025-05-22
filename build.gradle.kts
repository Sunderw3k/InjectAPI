import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.github.goooler.shadow") version "8.1.8"
    `maven-publish`
}

group = project.property("group") as String
version = project.property("version") as String
base.archivesName.set("InjectAPI")

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
        }
    }
}

dependencies {
    val asmVersion = "9.7"
    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("org.ow2.asm:asm-commons:$asmVersion")
    implementation("org.ow2.asm:asm-tree:$asmVersion")

    implementation("com.google.guava:guava:33.2.1-jre")

    testImplementation("net.bytebuddy:byte-buddy-agent:1.17.2")
    testImplementation(kotlin("test"))
}

sourceSets {
    create("testTargets") {
        java.srcDir("src/test-targets/java")
        kotlin.srcDir("src/test-targets/kotlin")
        resources.srcDir("src/test-targets/resources")
    }
}

tasks {
    val tokens = mapOf(
        "CONTEXT" to "rip/sunrise/injectapi/global/Context",
        "BOOTSTRAP" to "rip/sunrise/injectapi/global/ProxyDynamicFactory",
        "HOOK" to "rip/sunrise/injectapi/hooks/Hook",
        "HOOK_MANAGER" to "rip/sunrise/injectapi/managers/HookManager"
    )

    register<Sync>("processSourcesJava") {
        from(sourceSets.main.get().java)
        filter<ReplaceTokens>("tokens" to tokens)
        into("${layout.buildDirectory.get().asFile.absolutePath}/src/java")
    }
    register<Sync>("processSourcesKotlin") {
        from(sourceSets.main.get().kotlin)
        filter<ReplaceTokens>("tokens" to tokens)
        into("${layout.buildDirectory.get().asFile.absolutePath}/src/kotlin")
    }

    val buildTestTargetsJar = register<Jar>("buildTestTargetsJar") {
        archiveBaseName.set("test-targets")
        archiveVersion.set("")
        from(sourceSets["testTargets"].output)
    }

    test {
        dependsOn(buildTestTargetsJar)
        useJUnitPlatform()
    }

    compileJava {
        setSource(named("processSourcesJava").get().outputs)

        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    compileKotlin {
        val field = Class.forName("org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool").getDeclaredField("sourceFiles")
        field.isAccessible = true
        val source = field.get(this) as ConfigurableFileCollection
        source.setFrom(named("processSourcesKotlin"))

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "rip.sunrise.injectapi.shaded"

        relocate("kotlin", "kotlin")
    }

    build.get().finalizedBy(shadowJar)
}
