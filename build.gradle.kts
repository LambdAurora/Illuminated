import com.modrinth.minotaur.dependencies.ModDependency
import dev.lambdaurora.mcdev.api.McVersionLookup
import dev.lambdaurora.mcdev.api.ModUtils
import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
	id("fabric-loom").version("1.10.+")
	id("dev.lambdaurora.mcdev").version("1.0.+")
	id("dev.yumi.gradle.licenser").version("2.+")
	id("com.modrinth.minotaur").version("2.+")
	id("net.darkhax.curseforgegradle").version("1.1.+")
}

group = project.property("maven_group") as String
base.archivesName.set(project.property("archives_base_name") as String)

val mcVersion = libs.versions.minecraft.get()
val VERSION = project.property("mod_version") as String
version = "$VERSION+$mcVersion"

val targetJavaVersion = 21

repositories {
	mavenLocal()
	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/releases")
	}
	maven {
		name = "Gegy"
		url = uri("https://maven.gegy.dev/releases/")
	}
}

dependencies {
	minecraft(libs.minecraft)
	@Suppress("UnstableApiUsage")
	mappings(lambdamcdev.layered {
		officialMojangMappings()
		// Parchment is currently broken when used with the hacked mojmap layer due to remapping shenanigans.
		//parchment("org.parchmentmc.data:parchment-${mcVersion}:${project.property("parchment_mappings")}@zip")
		mappings("dev.lambdaurora:yalmm:${mcVersion}+build.${libs.versions.mappings.yalmm.get()}")
	})
	modImplementation(libs.fabric.loader)

	modImplementation(libs.fabric.api)

	modCompileOnly(libs.lambdynamiclights.api)
	modLocalRuntime(libs.lambdynamiclights.runtime)
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
		expand("version" to inputs.properties["version"])
	}
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

license {
	rule(rootProject.file("metadata/HEADER"))
}

modrinth {
	projectId = project.property("modrinth_id") as String
	versionName = "Illuminated ${project.version} (${McVersionLookup.getVersionTag(mcVersion)})"
	uploadFile.set(tasks.remapJar.get())
	loaders.set(listOf("fabric", "quilt"))
	gameVersions.set(listOf(mcVersion))
	versionType.set(ModUtils.fetchVersionType(VERSION, mcVersion))
	syncBodyFrom.set(
		ModUtils.parseReadme(
			project, "https://raw.githubusercontent.com/LambdAurora/Illuminated/1.21.4/\$2"
		)
	)
	dependencies.set(
		listOf(
			ModDependency("P7dR8mSH", "required"), // Fabric API
			ModDependency("yBW8D80W", "required"), // LambDynamicLights
			ModDependency("reCfnRvJ", "incompatible"),
			ModDependency("PxQSWIcD", "incompatible")
		)
	)

	// Changelog fetching
	val changelogContent = ModUtils.fetchChangelog(project, VERSION)

	if (changelogContent != null) {
		changelog.set(changelogContent)
	} else {
		afterEvaluate {
			tasks.modrinth.get().isEnabled = false
		}
	}
}

tasks.modrinth {
	dependsOn(tasks.modrinthSyncBody)
}

tasks.register<TaskPublishCurseForge>("curseforge") {
	this.group = "publishing"

	val token = System.getenv("CURSEFORGE_TOKEN")
	if (token != null) {
		this.apiToken = token
	} else {
		this.isEnabled = false
		return@register
	}

	// Changelog fetching
	var changelogContent = ModUtils.fetchChangelog(project, VERSION)

	if (changelogContent != null) {
		changelogContent = "Changelog:\n\n${changelogContent}"
	} else {
		this.isEnabled = false
		return@register
	}

	val mainFile = upload(project.property("curseforge_id"), tasks.remapJar.get())
	mainFile.releaseType = ModUtils.fetchVersionType(VERSION, mcVersion)
	mainFile.addGameVersion(McVersionLookup.getCurseForgeEquivalent(mcVersion))
	mainFile.addModLoader("Fabric", "Quilt")
	mainFile.addJavaVersion("Java 21", "Java 22")
	mainFile.addEnvironment("Client")

	mainFile.displayName = "Illuminated ${project.version} (${McVersionLookup.getVersionTag(mcVersion)})"
	mainFile.addRequirement("fabric-api")
	mainFile.addRequirement("lambdynamiclights")
	mainFile.addOptional("modmenu")
	mainFile.addIncompatibility("optifabric")

	mainFile.changelogType = "markdown"
	mainFile.changelog = changelogContent
}
