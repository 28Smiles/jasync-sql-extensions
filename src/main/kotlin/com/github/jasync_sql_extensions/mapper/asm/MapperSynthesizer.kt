package com.github.jasync_sql_extensions.mapper.asm

import com.github.jasync_sql_extensions.asm.DynamicClassLoader
import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.MapperCreator.CreatorIdentifier
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.lang.reflect.Constructor
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaType

internal object MapperSynthesizer {
    fun <Bean : Any> synthesize(creatorIdentifier: CreatorIdentifier<Bean>): Mapper<Bean> {
        val className = "${creatorIdentifier.specials.joinToString("")}${creatorIdentifier.clazz.java.name}\$Mapper"
        val primaryConstructor = creatorIdentifier.clazz.primaryConstructor
            ?: throw NullPointerException("No primary constructor found, is $creatorIdentifier not a Kotlin Class?")
        val defaultConstructor = creatorIdentifier.clazz.java.constructors.find { constructor ->
            constructor.parameters.any {
                it.type.name == "kotlin.jvm.internal.DefaultConstructorMarker"
            }
        }

        val classWriter = createClass(className) { classWriter ->
            createTypedConstructor(classWriter)
            createUntypedConstructMethod(className, creatorIdentifier.clazz.java, classWriter)
            createConstructMethod(
                creatorIdentifier,
                primaryConstructor,
                creatorIdentifier.clazz.java,
                defaultConstructor,
                classWriter
            )
        }

        val instance = DynamicClassLoader.defineClass(className, classWriter.toByteArray())!!
            .getConstructor(KClass::class.java, Set::class.java)
            .newInstance(creatorIdentifier.clazz, creatorIdentifier.specials)

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
            "(L${Type.getInternalName(KClass::class.java)};L${Type.getInternalName(Set::class.java)};)V",
            null,
            null
        )
        visitor.visitCode()
        visitor.visitVarInsn(Opcodes.ALOAD, 0)
        visitor.visitVarInsn(Opcodes.ALOAD, 1)
        visitor.visitVarInsn(Opcodes.ALOAD, 2)
        visitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            Type.getInternalName(Mapper::class.java),
            "<init>",
            "(L${Type.getInternalName(KClass::class.java)};L${Type.getInternalName(Set::class.java)};)V",
            false
        )
        visitor.visitInsn(Opcodes.RETURN)
        visitor.visitMaxs(2, 2)
    }

    private fun createUntypedConstructMethod(className: String, beanClass: Class<*>, classWriter: ClassWriter) {
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

    private fun <Bean : Any> createConstructMethod(
        creatorIdentifier: CreatorIdentifier<Bean>,
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
            if (creatorIdentifier.specials.contains(parameter.name) || Mapper.findMapper(parameter.type) != null) {
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
                    visitor.visitLdcInsn(
                        "Can not cast value \"null\" to \"${type.javaType}\" " +
                            "of field \"${parameter.name}\" of class \"${beanClass.name}\""
                    )
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
                val javaType = type.javaType
                if (javaType is Class<*>) {
                    AsmMapperCreator.javaBoxedToBase[type.javaType as Class<*>]?.let {
                        it(visitor)
                    }
                } else null ?: visitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(
                    when (javaType) {
                        is ParameterizedType -> javaType.rawType as Class<*>
                        is Class<*> -> javaType
                        else -> throw IllegalStateException("Could not extract raw type of $javaType")
                    }
                ))
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
