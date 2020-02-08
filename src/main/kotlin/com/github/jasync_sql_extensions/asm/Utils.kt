package com.github.jasync_sql_extensions.asm

import com.github.jasync.sql.db.util.length
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.AALOAD
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.BIPUSH
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.ICONST_0
import org.objectweb.asm.Opcodes.ICONST_1
import org.objectweb.asm.Opcodes.ICONST_2
import org.objectweb.asm.Opcodes.ICONST_3
import org.objectweb.asm.Opcodes.ICONST_4
import org.objectweb.asm.Opcodes.ICONST_5
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Opcodes.V1_8
import org.objectweb.asm.Type
import java.util.concurrent.TimeUnit
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaType

val classLoader = DynamicClassLoader()
val cache: LoadingCache<Pair<KClass<out Any>, KFunction<Any>>, Function<Array<Any?>, Any>> =
        CacheBuilder.newBuilder()
                .maximumSize(4096)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(object : CacheLoader<Pair<KClass<out Any>, KFunction<Any>>, Function<Array<Any?>, Any>>() {
                    override fun load(key: Pair<KClass<out Any>, KFunction<Any>>): Function<Array<Any?>, Any> {
                        return compile(key.first, key.second)
                    }
                })

fun <Bean : Any> compile(clazz: KClass<Bean>, constructor: KFunction<Any>): Function<Array<Any?>, Any> {
    val className = "${clazz.java.name}\$Mapper"
    val internalClassName = "${clazz.java.name.replace('.', '/')}\$Mapper"
    val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(
            V1_8,
            ACC_PUBLIC,
            internalClassName,
            null,
            "java/lang/Object",
            Array(1) { "java/util/function/Function" }
    )

    val constructorVisitor = {
        val visitor = classWriter.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        )
        visitor.visitCode()
        visitor.visitVarInsn(ALOAD, 0)
        visitor.visitMethodInsn(
                INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        )
        visitor.visitInsn(RETURN)
        visitor.visitMaxs(1, 1)
    }
    val applyVisitor = {
        val applyVisitor = classWriter.visitMethod(
                ACC_PUBLIC,
                "apply",
                "([Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
        )
        applyVisitor.visitCode()
        applyVisitor.visitTypeInsn(NEW, Type.getInternalName(clazz.java))
        applyVisitor.visitInsn(DUP)
        // Load array values

        constructor.parameters.forEachIndexed { index, parameter ->
            applyVisitor.visitVarInsn(ALOAD, 1) // ALOAD_1  Load Array Pointer
            when (index) {
                0 -> applyVisitor.visitInsn(ICONST_0)
                1 -> applyVisitor.visitInsn(ICONST_1)
                2 -> applyVisitor.visitInsn(ICONST_2)
                3 -> applyVisitor.visitInsn(ICONST_3)
                4 -> applyVisitor.visitInsn(ICONST_4)
                5 -> applyVisitor.visitInsn(ICONST_5)
                else -> applyVisitor.visitIntInsn(BIPUSH, index)
            }
            applyVisitor.visitInsn(AALOAD)
            applyVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(parameter.type.javaType as Class<*>))
        }
        // Call constructor
        applyVisitor.visitMethodInsn(
                INVOKESPECIAL,
                Type.getInternalName(clazz.java),
                "<init>",
                Type.getConstructorDescriptor(constructor.javaConstructor),
                false
        )
        applyVisitor.visitInsn(ARETURN)
        applyVisitor.visitMaxs(
                2 + constructor.parameters.length,
                2
        )
    }
    val applyOverrideVisitor = {
        val applyVisitor = classWriter.visitMethod(
                ACC_PUBLIC,
                "apply",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null
        )
        applyVisitor.visitVarInsn(ALOAD, 0)
        applyVisitor.visitVarInsn(ALOAD, 1)
        applyVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Array<Any>::class.java))
        applyVisitor.visitMethodInsn(
                INVOKEVIRTUAL,
                internalClassName,
                "apply",
                "([Ljava/lang/Object;)Ljava/lang/Object;",
                false
        )
        applyVisitor.visitInsn(ARETURN)
        applyVisitor.visitMaxs(
                2,
                2
        )
    }

    constructorVisitor()
    applyVisitor()
    applyOverrideVisitor()
    classWriter.visitEnd()

    return classLoader.defineClass(className, classWriter.toByteArray())!!
            .getConstructor().newInstance() as Function<Array<Any?>, Any>
}

inline fun <reified Bean : Any> createCompiledConstructor(constructor: KFunction<Bean>): Function<Array<Any?>, Bean> {
    return cache[Bean::class to constructor] as Function<Array<Any?>, Bean>
}
