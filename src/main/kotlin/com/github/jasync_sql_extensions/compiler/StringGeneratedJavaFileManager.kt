package com.github.jasync_sql_extensions.compiler

import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind

/**
 * @author Leon Camus
 * @since 07.02.2020
 */
class StringGeneratedJavaFileManager(
        fileManager: JavaFileManager?,
        val classLoader: StringGeneratedClassLoader
) : ForwardingJavaFileManager<JavaFileManager>(fileManager) {
    override fun getClassLoader(location: JavaFileManager.Location?): ClassLoader = classLoader
    override fun getJavaFileForOutput(
            location: JavaFileManager.Location?,
            className: String?,
            kind: Kind?,
            sibling: FileObject?
    ): JavaFileObject {
        require(!(kind !== Kind.CLASS)) {
            "Unsupported kind (" + kind.toString() + ") for class (" + className.toString() + ")."
        }
        val fileObject = StringGeneratedClassFileObject(className!!)
        classLoader.addJavaFileObject(className, fileObject)
        return fileObject
    }
}