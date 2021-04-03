package tech.poder.simpleAPI

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import java.nio.file.Paths

class APIPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("jarAPI", APIJar::class.java) { apiJar ->
            val jar = target.tasks.filterIsInstance<Jar>().first { it.name == "jar" }
            apiJar.setInput(jar)
            apiJar.setOutput(
                Paths.get(jar.destinationDirectory.get().asFile.absolutePath, "API-${jar.archiveFileName.get()}")
                    .toFile()
            )
        }
    }
}