package com.github.jasync_sql_extensions.compiler

import java.net.URI
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject

/**
 * @author Leon Camus
 * @since 07.02.2020
 */
class StringGeneratedSourceFileObject(fullClassName: String, val javaSource: String): SimpleJavaFileObject(
        URI.create("string:///" + fullClassName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE) {
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = javaSource
}