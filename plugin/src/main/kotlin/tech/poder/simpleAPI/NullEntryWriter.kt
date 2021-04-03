package tech.poder.simpleAPI

import org.gradle.internal.io.NullOutputStream
import proguard.io.DataEntry
import proguard.io.DataEntryWriter
import java.io.OutputStream
import java.io.PrintWriter

internal object NullEntryWriter : DataEntryWriter {
    override fun createDirectory(p0: DataEntry?): Boolean {
        return true
    }

    override fun sameOutputStream(p0: DataEntry?, p1: DataEntry?): Boolean {
        return p0 == p1
    }

    override fun createOutputStream(p0: DataEntry?): OutputStream {
        return NullOutputStream.INSTANCE
    }

    override fun close() {

    }

    override fun println(pw: PrintWriter?, prefix: String?) {

    }
}