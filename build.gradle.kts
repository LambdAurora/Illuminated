import com.modrinth.minotaur.dependencies.ModDependency
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.fabricmc.loom.api.mappings.layered.MappingContext
import net.fabricmc.loom.api.mappings.layered.MappingLayer
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec
import net.fabricmc.loom.configuration.providers.mappings.intermediary.IntermediaryMappingLayer
import net.fabricmc.loom.configuration.providers.mappings.utils.DstNameFilterMappingVisitor
import net.fabricmc.loom.util.download.DownloadException
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.MappingDstNsReorder
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

plugins {
	id("fabric-loom").version("1.9.+")
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

// Based off Loom, this is required as the releases at the time of writing this buildscript have
// a flaw with the mapping layering preventing the usage of the usual MojangMappingLayer.
@Suppress("UnstableApiUsage")
internal data class MojangMappingLayer(
	val clientMappings: Path, val serverMappings: Path, val nameSyntheticMembers: Boolean,
	val intermediaryMappings: MemoryMappingTree, val logger: Logger
) : MappingLayer {
	@Throws(IOException::class)
	override fun visit(mappingVisitor: MappingVisitor) {
		val mojmap = MemoryMappingTree()

		// Filter out field names matching the pattern
		val nameFilter = DstNameFilterMappingVisitor(mojmap, SYNTHETIC_NAME_PATTERN)

		// Make official the source namespace
		val nsSwitch = MappingSourceNsSwitch(if (nameSyntheticMembers) mojmap else nameFilter, MappingsNamespace.OFFICIAL.toString())

		Files.newBufferedReader(clientMappings).use { clientBufferedReader ->
			Files.newBufferedReader(serverMappings).use { serverBufferedReader ->
				ProGuardFileReader.read(
					clientBufferedReader,
					MappingsNamespace.NAMED.toString(),
					MappingsNamespace.OFFICIAL.toString(),
					nsSwitch
				)
				ProGuardFileReader.read(
					serverBufferedReader,
					MappingsNamespace.NAMED.toString(),
					MappingsNamespace.OFFICIAL.toString(),
					nsSwitch
				)
			}
		}

		intermediaryMappings.accept(MappingDstNsReorder(mojmap, MappingsNamespace.INTERMEDIARY.toString()))

		val switch = MappingSourceNsSwitch(MappingDstNsReorder(mappingVisitor, MappingsNamespace.NAMED.toString()), MappingsNamespace.INTERMEDIARY.toString(), true)
		mojmap.accept(switch)
	}

	override fun getSourceNamespace(): MappingsNamespace {
		return MappingsNamespace.INTERMEDIARY
	}

	override fun dependsOn(): List<Class<out MappingLayer?>> {
		return listOf(IntermediaryMappingLayer::class.java)
	}

	companion object {
		private val SYNTHETIC_NAME_PATTERN: Pattern = Pattern.compile("^(access|this|val\\\$this|lambda\\$.*)\\$[0-9]+$")
	}
}

@Suppress("UnstableApiUsage")
internal data class MojangMappingsSpec(val nameSyntheticMembers: Boolean) : MappingsSpec<MojangMappingLayer?> {
	override fun createLayer(context: MappingContext): MojangMappingLayer {
		val versionInfo = context.minecraftProvider().versionInfo
		val clientDownload = versionInfo.download(MANIFEST_CLIENT_MAPPINGS)
		val serverDownload = versionInfo.download(MANIFEST_SERVER_MAPPINGS)

		if (clientDownload == null) {
			throw RuntimeException("Failed to find official mojang mappings for " + context.minecraftVersion())
		}

		val clientMappings = context.workingDirectory("mojang").resolve("client.txt")
		val serverMappings = context.workingDirectory("mojang").resolve("server.txt")

		try {
			context.download(clientDownload.url())
				.sha1(clientDownload.sha1())
				.downloadPath(clientMappings)

			context.download(serverDownload.url())
				.sha1(serverDownload.sha1())
				.downloadPath(serverMappings)
		} catch (e: DownloadException) {
			throw UncheckedIOException("Failed to download mappings", e)
		}

		return MojangMappingLayer(
			clientMappings,
			serverMappings,
			nameSyntheticMembers,
			context.intermediaryTree().get(),
			context.logger
		)
	}

	companion object {
		// Keys in dependency manifest
		private const val MANIFEST_CLIENT_MAPPINGS = "client_mappings"
		private const val MANIFEST_SERVER_MAPPINGS = "server_mappings"
	}
}

dependencies {
	minecraft(libs.minecraft)
	@Suppress("UnstableApiUsage")
	mappings(loom.layered {
		addLayer(MojangMappingsSpec(false))
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
		expand("version" to project.version)
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

fun isMcVersionNonRelease(): Boolean {
	return this.mcVersion.matches(Regex("^\\d\\dw\\d\\d[a-z]$"))
			|| this.mcVersion.matches(Regex("\\d+\\.\\d+-(pre|rc)(\\d+)"))
}

fun getMcVersionString(): String {
	if (isMcVersionNonRelease()) {
		return this.mcVersion
	}
	val version = this.mcVersion.split("\\.".toRegex())
	return version[0] + "." + version[1]
}

fun fetchVersionType(): String {
	return if (this.isMcVersionNonRelease() || "-alpha." in VERSION) {
		"alpha"
	} else if ("-beta." in VERSION) {
		"beta"
	} else {
		"release"
	}
}

fun parseReadme(project: Project): String {
	val linkRegex = "!\\[(.+?)]\\((assets\\/[A-z.\\/_]+)\\)"

	var readme = project.rootProject.file("README.md").readText()
	val lines = readme.split("\n").toMutableList()
	val it = lines.listIterator()

	var shouldRemove = false;
	while (it.hasNext()) {
		val line = it.next();

		if (line == "<!-- modrinth_exclude.long_start -->") {
			shouldRemove = true
		}

		if (shouldRemove) {
			it.remove()
		}

		if (line == "<!-- modrinth_exclude.long_end -->") {
			shouldRemove = false
		}
	}

	readme = lines.joinToString("\n")
	readme = readme.replace(linkRegex.toRegex(), "![\$1](https://raw.githubusercontent.com/LambdAurora/Illuminated/1.21.4/\$2)")
	readme = readme.replace("<!-- modrinth_only.start ", "")
	readme = readme.replace(" modrinth_only.end -->", "")
	return readme
}

fun fetchChangelog(project: Project): String? {
	val changelogText = project.rootProject.file("CHANGELOG.md").readText()
	val regexVersion = VERSION.replace("\\.".toRegex(), "\\.").replace("\\+".toRegex(), "\\+")
	val changelogRegex = "###? ${regexVersion}\\n\\n(( *- .+\\n)+)".toRegex()
	val matcher = changelogRegex.find(changelogText)

	if (matcher != null) {
		var changelogContent = matcher.groupValues[1]

		val changelogLines = changelogText.split("\n")
		val linkRefRegex = "^\\[([A-z\\d _\\-/+.]+)]: ".toRegex()
		for (i in changelogLines.size - 1 downTo 0) {
			val line = changelogLines[i]
			if (line matches linkRefRegex)
				changelogContent += "\n" + line
			else break
		}
		return changelogContent
	} else {
		return null;
	}
}

modrinth {
	projectId = project.property("modrinth_id") as String
	versionName = "Illuminated ${project.version} (${mcVersion})"
	uploadFile.set(tasks.remapJar.get())
	loaders.set(listOf("fabric", "quilt"))
	gameVersions.set(listOf(mcVersion))
	versionType.set(fetchVersionType())
	syncBodyFrom.set(parseReadme(project))
	dependencies.set(
		listOf(
			ModDependency("P7dR8mSH", "required"), // Fabric API
			ModDependency("yBW8D80W", "required")  // LambDynamicLights
		)
	)

	// Changelog fetching
	val changelogContent = fetchChangelog(project)

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
	var changelogContent = fetchChangelog(project)

	if (changelogContent != null) {
		changelogContent = "Changelog:\n\n${changelogContent}"
	} else {
		this.isEnabled = false
		return@register
	}

	val mainFile = upload(project.property("curseforge_id"), tasks.remapJar.get())
	mainFile.releaseType = fetchVersionType()
	mainFile.addGameVersion(mcVersion)
	mainFile.addModLoader("Fabric", "Quilt")
	mainFile.addJavaVersion("Java 21", "Java 22")
	mainFile.addEnvironment("Client")

	mainFile.displayName = "Illuminated ${project.version} (${mcVersion})"
	mainFile.addRequirement("fabric-api")
	mainFile.addRequirement("lambdynamiclights")
	mainFile.addOptional("modmenu")
	mainFile.addIncompatibility("optifabric")

	mainFile.changelogType = "markdown"
	mainFile.changelog = changelogContent
}
