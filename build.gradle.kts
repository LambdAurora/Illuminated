import dev.lambdaurora.mcdev.api.EnvironmentType
import dev.lambdaurora.mcdev.api.McVersionLookup
import dev.lambdaurora.mcdev.api.ModUtils
import dev.lambdaurora.mcdev.api.ModVersionDependency
import dev.lambdaurora.mcdev.task.packaging.PackageModrinthTask

plugins {
	id("net.fabricmc.fabric-loom").version("1.17.+")
	id("dev.lambdaurora.mcdev").version("3.0.1+")
	id("dev.yumi.gradle.licenser").version("4.+")
}

group = project.property("maven_group") as String
base.archivesName.set(project.property("archives_base_name") as String)

val mcVersion = libs.versions.minecraft.get()
val VERSION = project.property("mod_version") as String
version = "$VERSION+$mcVersion"

val targetJavaVersion = 25

repositories {
	mavenLocal()
	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/releases")
	}
	exclusiveContent {
		filter {
			includeGroupAndSubgroups("dev.lambdaurora")
			includeGroup("io.github.queerbric")
		}
		forRepository {
			maven {
				name = "Gegy"
				url = uri("https://maven.gegy.dev/releases/")
			}
		}
	}
}

loom {
	accessWidenerPath = file("src/main/resources/illuminated.accesswidener")
}

dependencies {
	minecraft(libs.minecraft)
	implementation(libs.fabric.loader)

	implementation(libs.fabric.api)
	implementation(libs.yumi.mc.foundation)
	include(libs.yumi.mc.foundation)

	compileOnly(libs.lambdynamiclights.api)
	localRuntime(libs.lambdynamiclights.runtime)
}

java {
	sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
	targetCompatibility = JavaVersion.toVersion(targetJavaVersion)

	withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.isDeprecation = true
	options.isIncremental = true
	options.release.set(targetJavaVersion)
}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand("version" to (inputs.properties["version"] as String))
	}
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

license {
	rule(rootProject.file("metadata/HEADER"))

	include("**/*.java")
}

val packageModrinthTask = tasks.register<PackageModrinthTask>("packageModrinth") {
	this.group = "publishing"
	this.versionType.set(ModUtils.getVersionType(VERSION, mcVersion))
	this.versionName.set("Illuminated $VERSION (${McVersionLookup.getVersionTag(mcVersion)})")
	this.gameVersions.set(listOf(mcVersion))
	this.loaders.set(listOf("fabric", "quilt"))
	this.environment.set(EnvironmentType.CLIENT_AND_SERVER)
	this.dependencies.set(
		listOf(
			ModVersionDependency("P7dR8mSH", ModVersionDependency.Type.REQUIRED), // Fabric API
			ModVersionDependency("yBW8D80W", ModVersionDependency.Type.REQUIRED), // LambDynamicLights
			ModVersionDependency("reCfnRvJ", ModVersionDependency.Type.INCOMPATIBLE),
			ModVersionDependency("PxQSWIcD", ModVersionDependency.Type.INCOMPATIBLE)
		)
	)
	this.changelog.set(ModUtils.fetchChangelog(project, VERSION))
	this.readme.set(
		ModUtils.parseReadme(
			project, "https://raw.githubusercontent.com/LambdAurora/Illuminated/26.3/\$2"
		)
	)
	this.files.setFrom(tasks.jar)
}
