import com.modrinth.minotaur.dependencies.ModDependency
import dev.lambdaurora.mcdev.api.McVersionLookup
import dev.lambdaurora.mcdev.api.ModUtils
import dev.lambdaurora.mcdev.api.ModVersionDependency
import dev.lambdaurora.mcdev.task.packaging.PackageModrinthTask
import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
	id("net.fabricmc.fabric-loom").version("1.15.+")
	id("dev.lambdaurora.mcdev").version("2.0.+")
	id("dev.yumi.gradle.licenser").version("2.+")
	id("com.modrinth.minotaur").version("2.+")
	id("net.darkhax.curseforgegradle").version("1.1.+")
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
	maven {
		name = "Gegy"
		url = uri("https://maven.gegy.dev/releases/")
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
}

val packageModrinth by tasks.registering(PackageModrinthTask::class) {
	this.group = "publishing"
	this.versionType.set(ModUtils.getVersionType(VERSION, mcVersion))
	this.versionName.set("Illuminated $VERSION (${McVersionLookup.getVersionTag(mcVersion)})")
	this.gameVersions.set(listOf(mcVersion))
	this.loaders.set(listOf("fabric", "quilt"))
	this.dependencies.set(
		listOf(
			ModVersionDependency("P7dR8mSH", ModVersionDependency.Type.REQUIRED), // Fabric API
			ModVersionDependency("yBW8D80W", ModVersionDependency.Type.REQUIRED), // LambDynamicLights
			ModVersionDependency("reCfnRvJ", ModVersionDependency.Type.INCOMPATIBLE),
			ModVersionDependency("PxQSWIcD", ModVersionDependency.Type.INCOMPATIBLE)
		)
	)
	this.changelog.set(ModUtils.fetchChangelog(project, VERSION))
	this.readme.set(ModUtils.parseReadme(
		project, "https://raw.githubusercontent.com/LambdAurora/Illuminated/26.1/\$2"
	))
	this.files.setFrom(tasks.jar)
}

modrinth {
	projectId = project.property("modrinth_id") as String
	versionName = "Illuminated $VERSION (${McVersionLookup.getVersionTag(mcVersion)})"
	uploadFile.set(tasks.jar.get())
	loaders.set(listOf("fabric", "quilt"))
	gameVersions.set(listOf(mcVersion))
	versionType.set(ModUtils.fetchVersionType(VERSION, mcVersion))
	syncBodyFrom.set(
		ModUtils.parseReadme(
			project, "https://raw.githubusercontent.com/LambdAurora/Illuminated/26.1/\$2"
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

	val mainFile = upload(project.property("curseforge_id"), tasks.jar.get())
	mainFile.releaseType = ModUtils.fetchVersionType(VERSION, mcVersion)
	mainFile.addGameVersion(McVersionLookup.getCurseForgeEquivalent(mcVersion))
	mainFile.addModLoader("Fabric", "Quilt")
	mainFile.addJavaVersion("Java 21", "Java 22")
	mainFile.addEnvironment("Client")

	mainFile.displayName = "Illuminated $VERSION (${McVersionLookup.getVersionTag(mcVersion)})"
	mainFile.addRequirement("fabric-api")
	mainFile.addRequirement("lambdynamiclights")
	mainFile.addOptional("modmenu")
	mainFile.addIncompatibility("optifabric")

	mainFile.changelogType = "markdown"
	mainFile.changelog = changelogContent
}
