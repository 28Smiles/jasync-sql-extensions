package com.github.jasync_sql_extensions.mapper.asm

import com.github.jasync_sql_extensions.asm.DynamicClassLoader
import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.Mapper.Companion.nativeTypes
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.AALOAD
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.ATHROW
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.ICONST_0
import org.objectweb.asm.Opcodes.IFNONNULL
import org.objectweb.asm.Opcodes.ILOAD
import org.objectweb.asm.Opcodes.INVOKEINTERFACE
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Type
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmName

val classLoader = DynamicClassLoader()
val cache: LoadingCache<KClass<out Any>, Mapper<out Any>> =
        CacheBuilder.newBuilder()
                .maximumSize(4096)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(object : CacheLoader<KClass<out Any>, Mapper<out Any>>() {
                    override fun load(key: KClass<out Any>): Mapper<out Any> {
                        return create(key)
                    }
                })

private fun <Bean : Any> create(clazz: KClass<Bean>): Mapper<Bean> {
    val className = "${clazz.java.name}\$Mapper"
    val internalClassName = className.replace('.', '/')
    val primaryConstructor = clazz.primaryConstructor!!
    val defaultConstructor = clazz.java.constructors.find { constructor ->
        constructor.parameters.any {
            it.type.name == "kotlin.jvm.internal.DefaultConstructorMarker"
        }
    }

    val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            internalClassName,
            null,
            Mapper::class.java.name.replace('.', '/'),
            Array(0) { "" }
    )

    val writeConstructor = {
        val visitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lkotlin/reflect/KClass;)V",
                null,
                null
        )
        visitor.visitCode()
        visitor.visitVarInsn(ALOAD, 0)
        visitor.visitVarInsn(ALOAD, 1)
        visitor.visitMethodInsn(
                INVOKESPECIAL,
                Mapper::class.java.name.replace('.', '/'),
                "<init>",
                "(Lkotlin/reflect/KClass;)V",
                false
        )
        visitor.visitInsn(Opcodes.RETURN)
        visitor.visitMaxs(2, 2)
    }

    val writeIConst = { visitor: MethodVisitor, i: Int ->
        when (i) {
            0 -> visitor.visitInsn(ICONST_0)
            1 -> visitor.visitInsn(Opcodes.ICONST_1)
            2 -> visitor.visitInsn(Opcodes.ICONST_2)
            3 -> visitor.visitInsn(Opcodes.ICONST_3)
            4 -> visitor.visitInsn(Opcodes.ICONST_4)
            5 -> visitor.visitInsn(Opcodes.ICONST_5)
            else -> visitor.visitIntInsn(Opcodes.BIPUSH, i)
        }
    }

    val writeConstructMethod = {
        val visitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "construct",
                "(Lcom/github/jasync/sql/db/RowData;I[Lkotlin/jvm/functions/Function1;)" +
                        "L${clazz.java.name.replace('.', '/')};",
                null,
                null
        )
        visitor.visitCode()
        // ALLOCATE
        visitor.visitTypeInsn(NEW, Type.getInternalName(clazz.java))
        visitor.visitInsn(DUP)
        // Load
        primaryConstructor.parameters.forEachIndexed { i, parameter ->
            val type = parameter.type
            val mapper = nativeTypes[parameter.type]
            if (mapper != null) {
                visitor.visitVarInsn(ALOAD, 3) // LOAD LAMBDA ARRAY
                writeIConst(visitor, i) // LOAD OFFSET
                visitor.visitInsn(AALOAD) // LOAD LAMBDA ADDRESS
                // STACK: lambda
                visitor.visitVarInsn(ALOAD, 1)
                // STACK: lambda, rowData
                visitor.visitMethodInsn(
                        INVOKEINTERFACE,
                        "kotlin/jvm/functions/Function1",
                        "invoke",
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        true
                )
                if (!type.isMarkedNullable) {
                    // NULLCHECK
                    val label = Label()
                    visitor.visitInsn(DUP)
                    visitor.visitJumpInsn(IFNONNULL, label)
                    // CREATE EXCEPTION
                    visitor.visitTypeInsn(NEW, Type.getInternalName(NullPointerException::class.java))
                    visitor.visitInsn(DUP)
                    visitor.visitLdcInsn("Can not cast value \"null\" to \"${type.javaType}\" " +
                            "of field \"${parameter.name}\" of class \"${clazz.jvmName}\"")
                    visitor.visitMethodInsn(
                            INVOKESPECIAL,
                            Type.getInternalName(NullPointerException::class.java),
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false
                    )
                    // THROW
                    visitor.visitInsn(ATHROW)
                    visitor.visitLabel(label)
                }
                javaBoxedToBase[type.javaType as Class<*>]?.let {
                    it(visitor)
                } ?: visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(type.javaType as Class<*>))
            } else {
                visitor.visitInsn(ACONST_NULL)
            }
        }

        if (defaultConstructor != null) {
            // Load information
            visitor.visitVarInsn(ILOAD, 2)
            visitor.visitInsn(ACONST_NULL)
            // Construct
            visitor.visitMethodInsn(
                    INVOKESPECIAL,
                    Type.getInternalName(clazz.java),
                    "<init>",
                    Type.getConstructorDescriptor(defaultConstructor),
                    false
            )
            visitor.visitInsn(ARETURN)
            visitor.visitMaxs(primaryConstructor.parameters.size + 3, 3)
        } else {
            // Construct
            visitor.visitMethodInsn(
                    INVOKESPECIAL,
                    Type.getInternalName(clazz.java),
                    "<init>",
                    Type.getConstructorDescriptor(primaryConstructor.javaConstructor),
                    false
            )
            visitor.visitInsn(ARETURN)
            visitor.visitMaxs(primaryConstructor.parameters.size, 3)
        }
    }

    val writeContructObjectMethod = {
        val visitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "construct",
                "(Lcom/github/jasync/sql/db/RowData;I[Lkotlin/jvm/functions/Function1;)" +
                        "Ljava/lang/Object;",
                null,
                null
        )
        visitor.visitCode()
        visitor.visitVarInsn(ALOAD, 0)
        visitor.visitVarInsn(ALOAD, 1)
        visitor.visitVarInsn(ILOAD, 2)
        visitor.visitVarInsn(ALOAD, 3)
        visitor.visitMethodInsn(
                INVOKEVIRTUAL,
                internalClassName,
                "construct",
                "(Lcom/github/jasync/sql/db/RowData;I[Lkotlin/jvm/functions/Function1;)" +
                        "L${clazz.java.name.replace('.', '/')};",
                false
        )
        visitor.visitInsn(ARETURN)
        visitor.visitMaxs(4, 4)
    }

    writeConstructor()
    writeConstructMethod()
    writeContructObjectMethod()
    classWriter.visitEnd()

    //FileOutputStream(className.split('.').last() + ".class").let {
    //    it.write(classWriter.toByteArray())
    //    it.close()
    //}

    val instance = classLoader.defineClass(className, classWriter.toByteArray())!!
            .getConstructor(KClass::class.java).newInstance(clazz)

    return instance as Mapper<Bean>
}

fun unbox(boxed: Class<*>, unboxed: Type, method: String): (MethodVisitor) -> Unit = { visitor ->
    visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(boxed))
    visitor.visitMethodInsn(
            INVOKEVIRTUAL,
            Type.getInternalName(boxed),
            method,
            Type.getMethodDescriptor(
                    unboxed
            ),
            false
    )
}

val javaBoxedToBase = mapOf<Class<*>, (MethodVisitor) -> Unit>(
        Long::class.java to unbox(java.lang.Long::class.java, Type.LONG_TYPE, "longValue"),
        Int::class.java to unbox(java.lang.Integer::class.java, Type.INT_TYPE, "intValue"),
        Float::class.java to unbox(java.lang.Float::class.java, Type.FLOAT_TYPE, "floatValue"),
        Double::class.java to unbox(java.lang.Double::class.java, Type.DOUBLE_TYPE, "doubleValue"),
        Byte::class.java to unbox(java.lang.Byte::class.java, Type.BYTE_TYPE, "byteValue"),
        Boolean::class.java to unbox(java.lang.Boolean::class.java, Type.BOOLEAN_TYPE, "booleanValue"),
        Char::class.java to unbox(java.lang.Character::class.java, Type.CHAR_TYPE, "charValue"),
        Short::class.java to unbox(java.lang.Short::class.java, Type.SHORT_TYPE, "shortValue")
)
