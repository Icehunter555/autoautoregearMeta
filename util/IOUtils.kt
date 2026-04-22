package dev.wizard.meta.util

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringWriter
import java.nio.charset.Charset

fun InputStream.readText(charset: Charset = Charsets.UTF_8, bufferSize: Int = 8192): String {
    val stringWriter = StringWriter()
    val bufferedInputStream = if (this is BufferedInputStream) this else BufferedInputStream(this, bufferSize / 2)
    val reader = InputStreamReader(bufferedInputStream, charset)
    reader.copyTo(stringWriter, bufferSize / 2)
    return stringWriter.toString()
}
