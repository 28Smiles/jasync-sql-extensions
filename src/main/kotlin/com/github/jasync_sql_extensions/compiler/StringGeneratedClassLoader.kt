package com.github.jasync_sql_extensions.compiler

import java.util.*


/**
 * @author Leon Camus
 * @since 07.02.2020
 */
class StringGeneratedClassLoader(parent: ClassLoader?): ClassLoader(parent) {
    private val fileObjectMap: MutableMap<String, StringGeneratedClassFileObject> = HashMap()

    @Throws(ClassNotFoundException::class)
    override fun findClass(fullClassName: String?): Class<*>? {
        val fileObject: StringGeneratedClassFileObject? = fileObjectMap.get(fullClassName)
        if (fileObject != null) {
            val classBytes: ByteArray? = fileObject.getClassBytes()
            return defineClass(fullClassName, classBytes, 0, classBytes!!.size)
        }
        return super.findClass(fullClassName)
    }

    fun addJavaFileObject(qualifiedName: String, fileObject: StringGeneratedClassFileObject) {
        fileObjectMap[qualifiedName] = fileObject
    }
}