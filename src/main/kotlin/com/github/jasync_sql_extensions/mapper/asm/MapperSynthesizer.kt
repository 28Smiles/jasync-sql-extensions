package com.github.jasync_sql_extensions.mapper.asm

import com.github.jasync_sql_extensions.asm.DynamicClassLoader
import com.github.jasync_sql_extensions.mapper.Mapper
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.lang.reflect.Constructor
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaType

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
internal object MapperSynthesizer {
    fun <Bean : Any> synthesize(clazz: KClass<Bean>): Mapper<Bean> {
        val className = "${clazz.java.name}\$Mapper"
        val primaryConstructor = clazz.primaryConstructor
                ?: throw NullPointerException("No primary constructor found, is $clazz not a Kotlin Class?")
        val defaultConstructor = clazz.java.constructors.find { constructor ->
            constructor.parameters.any {
                it.type.name == "kotlin.jvm.internal.DefaultConstructorMarker"
            }
        }

        val classWriter = createClass(className) { classWriter ->
            createTypedConstructor(classWriter)
            createUntypedConstructor(className, clazz.java, classWriter)
            createConstructMethod(primaryConstructor, clazz.java, defaultConstructor, classWriter)
        }

        val instance = DynamicClassLoader.defineClass(className, classWriter.toByteArray())!!
                .getConstructor(KClass::class.java).newInstance(clazz)

        @Suppress("UNCHECKED_CAST")
        return instance as Mapper<Bean>
    }

    private fun createClass(className: String, inner: (ClassWriter) -> Unit): ClassWriter {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        classWriter.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                className.replace('.', '/'),
                null,
                Mapper::class.java.name.replace('.', '/'),
                Array(0) { "" }
        )

        inner(classWriter)

        classWriter.visitEnd()

        return classWriter
    }

    private fun createTypedConstructor(classWriter: ClassWriter) {
        val visitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lkotlin/reflect/KClass;)V",
                null,
                null
        )
        visitor.visitCode()
        visitor.visitVarInsn(Opcodes.ALOAD, 0)
        visitor.visitVarInsn(Opcodes.ALOAD, 1)
        visitor.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                Type.getInternalName(Mapper::class.java),
                "<init>",
                "(Lkotlin/reflect/KClass;)V",
                false
        )
        visitor.visitInsn(Opcodes.RETURN)
        visitor.visitMaxs(2, 2)
    }

    private fun createUntypedConstructor(className: String, beanClass: Class<*>, classWriter: ClassWriter) {
        val visitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "construct",
                "(Lcom/github/jasync/sql/db/RowData;I[Lkotlin/jvm/functions/Function1;)" +
                        "Ljava/lang/Object;",
                null,
                null
        )
        visitor.visitCode()
        visitor.visitVarInsn(Opcodes.ALOAD, 0)
        visitor.visitVarInsn(Opcodes.ALOAD, 1)
        visitor.visitVarInsn(Opcodes.ILOAD, 2)
        visitor.visitVarInsn(Opcodes.ALOAD, 3)
        visitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                className.replace('.', '/'),
                "construct",
                "(Lcom/github/jasync/sql/db/RowData;I[Lkotlin/jvm/functions/Function1;)" +
                        "L${Type.getInternalName(beanClass)};",
                false
        )
        visitor.visitInsn(Opcodes.ARETURN)
        visitor.visitMaxs(4, 4)
    }

    private fun createConstructMethod(
            primaryConstructor: KFunction<Any>,
            beanClass: Class<*>,
            defaultConstructor: Constructor<*>?,
            classWriter: ClassWriter
    ) {
        val visitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "construct",
                "(Lcom/github/jasync/sql/db/RowData;I[Lkotlin/jvm/functions/Function1;)" +
                        "L${Type.getInternalName(beanClass)};",
                null,
                null
        )
        visitor.visitCode()
        // ALLOCATE
        visitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(beanClass))
        visitor.visitInsn(Opcodes.DUP)
        // Load
        primaryConstructor.parameters.forEachIndexed { i, parameter ->
            val type = parameter.type
            val mapper = Mapper.findMapper(parameter.type)
            if (mapper != null) {
                visitor.visitVarInsn(Opcodes.ALOAD, 3) // LOAD LAMBDA ARRAY
                visitor.visitIConst(i) // LOAD OFFSET
                visitor.visitInsn(Opcodes.AALOAD) // LOAD LAMBDA ADDRESS
                // STACK: lambda
                visitor.visitVarInsn(Opcodes.ALOAD, 1)
                // STACK: lambda, rowData
                visitor.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "kotlin/jvm/functions/Function1",
                        "invoke",
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        true
                )
                if (!type.isMarkedNullable) {
                    // NULLCHECK
                    val label = Label()
                    visitor.visitInsn(Opcodes.DUP)
                    visitor.visitJumpInsn(Opcodes.IFNONNULL, label)
                    // CREATE EXCEPTION
                    visitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(NullPointerException::class.java))
                    visitor.visitInsn(Opcodes.DUP)
                    visitor.visitLdcInsn("Can not cast value \"null\" to \"${type.javaType}\" " +
                            "of field \"${parameter.name}\" of class \"${beanClass.name}\"")
                    visitor.visitMethodInsn(
                            Opcodes.INVOKESPECIAL,
                            Type.getInternalName(NullPointerException::class.java),
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false
                    )
                    // THROW
                    visitor.visitInsn(Opcodes.ATHROW)
                    visitor.visitLabel(label)
                }
                AsmMapperCreator.javaBoxedToBase[type.javaType as Class<*>]?.let {
                    it(visitor)
                } ?: visitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type.javaType as Class<*>))
            } else {
                visitor.visitInsn(Opcodes.ACONST_NULL)
            }
        }

        if (defaultConstructor != null) {
            // Load information
            visitor.visitVarInsn(Opcodes.ILOAD, 2)
            visitor.visitInsn(Opcodes.ACONST_NULL)
            // Construct
            visitor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(beanClass),
                    "<init>",
                    Type.getConstructorDescriptor(defaultConstructor),
                    false
            )
            visitor.visitInsn(Opcodes.ARETURN)
            visitor.visitMaxs(primaryConstructor.parameters.size + 3, 3)
        } else {
            // Construct
            visitor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(beanClass),
                    "<init>",
                    Type.getConstructorDescriptor(primaryConstructor.javaConstructor),
                    false
            )
            visitor.visitInsn(Opcodes.ARETURN)
            visitor.visitMaxs(primaryConstructor.parameters.size, 3)
        }
    }

    private fun MethodVisitor.visitIConst(i: Int) {
        when (i) {
            0 -> this.visitInsn(Opcodes.ICONST_0)
            1 -> this.visitInsn(Opcodes.ICONST_1)
            2 -> this.visitInsn(Opcodes.ICONST_2)
            3 -> this.visitInsn(Opcodes.ICONST_3)
            4 -> this.visitInsn(Opcodes.ICONST_4)
            5 -> this.visitInsn(Opcodes.ICONST_5)
            else -> this.visitIntInsn(Opcodes.BIPUSH, i)
        }
    }
}
