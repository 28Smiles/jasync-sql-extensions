package com.github.jasync_sql_extensions.compiler

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject


/**
 * @author Leon Camus
 * @since 07.02.2020
 */
class StringGeneratedClassFileObject(fullClassName: String): SimpleJavaFileObject(
        URI.create("bytes:///" + fullClassName), Kind.CLASS
) {
    private var classOutputStream: ByteArrayOutputStream? = null

    override fun openInputStream(): InputStream? = ByteArrayInputStream(getClassBytes())

    override fun openOutputStream(): OutputStream? {
        classOutputStream = ByteArrayOutputStream()
        return classOutputStream
    }

    fun getClassBytes(): ByteArray? = classOutputStream?.toByteArray()
}