import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import refbyname.GenerateRefByNameRulesTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    `maven-publish`
}

group = project.property("group") as String
version = project.property("version") as String
base.archivesName.set("InjectAPI")

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.asm.commons)

    testImplementation("net.bytebuddy:byte-buddy-agent:1.17.2")
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
        }
    }
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

    val generateRefByNameRules = register<GenerateRefByNameRulesTask>("generateRefByNameRules") {
        classDirs.from(
            named("compileJava").map { it.outputs.files },
            named("compileKotlin").map { it.outputs.files },
        )
        output.set(layout.buildDirectory.file("generated/proguard/ref-by-name.pro"))
    }

    processResources {
        from(generateRefByNameRules.flatMap { it.output }) {
            into("META-INF/proguard")
        }
    }

    shadowJar {
        enableAutoRelocation = true
        relocationPrefix = "rip.sunrise.injectapi.shaded"

        relocate("kotlin", "kotlin")
    }
}
