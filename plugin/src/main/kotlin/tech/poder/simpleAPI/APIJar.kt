package tech.poder.simpleAPI

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import proguard.io.*
import proguard.util.StringFunction
import java.io.File

open class APIJar : DefaultTask() {
    private var destination: File? = null
    private var input: Jar? = null

    fun setInput(jar: Jar) {
        input = jar
    }

    fun setOutput(file: File) {
        destination = file
    }

    @TaskAction
    fun apify() {

        if (destination!!.exists()) {
            destination!!.delete()
        }

        val source: DataEntrySource = FileSource(
            input!!.archiveFile.get().asFile
        )

        val jarWriter = JarWriter(
            arrayOf("SHA-256"), null, "META-INF/MANIFEST.MF", StringFunction.IDENTITY_FUNCTION,
            ZipWriter(
                FixedFileWriter(
                    destination!!
                )
            ),
            NullEntryWriter
        )

        source.pumpDataEntries {
            JarReader(
                ClassFilter(
                    ClassReader(
                        false, true, true, false, null,
                        MyLovelyVisitor(DataEntryClassWriter(jarWriter))
                    )
                )
            ).read(it)
        }

        jarWriter.close()
    }
}