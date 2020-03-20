package com.github.jasync_sql_extensions.mapper.asm

import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.MapperCreator
import com.github.jasync_sql_extensions.mapper.MapperCreator.CreatorIdentifier
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.reflect.KClass

object AsmMapperCreator: MapperCreator {
    private val cache: LoadingCache<CreatorIdentifier<out Any>, Mapper<out Any>> =
            CacheBuilder.newBuilder()
                    .build(object : CacheLoader<CreatorIdentifier<out Any>, Mapper<out Any>>() {
                        override fun load(key: CreatorIdentifier<out Any>): Mapper<out Any> {
                            val className = "${key.clazz.java.packageName}.${key.specials.sorted().joinToString("")}${key.clazz.java.name.substringAfterLast(".")}\$Mapper"
                            return try {
                                val clazz = Class.forName(className) as Class<Mapper<out Any>>
                                clazz.getConstructor(KClass::class.java, Set::class.java)
                                    .newInstance(key.clazz, key.specials)
                            } catch (e: ClassNotFoundException) {
                                MapperSynthesizer.synthesize(key, className)
                            }
                        }
                    })

    internal val javaBoxedToBase = mapOf<Class<*>, (MethodVisitor) -> Unit>(
            Long::class.java to unbox(java.lang.Long::class.java, Type.LONG_TYPE, "longValue"),
            Int::class.java to unbox(java.lang.Integer::class.java, Type.INT_TYPE, "intValue"),
            Float::class.java to unbox(java.lang.Float::class.java, Type.FLOAT_TYPE, "floatValue"),
            Double::class.java to unbox(java.lang.Double::class.java, Type.DOUBLE_TYPE, "doubleValue"),
            Byte::class.java to unbox(java.lang.Byte::class.java, Type.BYTE_TYPE, "byteValue"),
            Boolean::class.java to unbox(java.lang.Boolean::class.java, Type.BOOLEAN_TYPE, "booleanValue"),
            Char::class.java to unbox(java.lang.Character::class.java, Type.CHAR_TYPE, "charValue"),
            Short::class.java to unbox(java.lang.Short::class.java, Type.SHORT_TYPE, "shortValue")
    )

    @Suppress("UNCHECKED_CAST")
    override fun <Bean: Any>get(creatorIdentifier: CreatorIdentifier<Bean>): Mapper<Bean>
        = cache[creatorIdentifier] as Mapper<Bean>

    fun unbox(boxed: Class<*>, unboxed: Type, method: String): (MethodVisitor) -> Unit = { visitor ->
        visitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(boxed))
        visitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(boxed),
                method,
                Type.getMethodDescriptor(
                        unboxed
                ),
                false
        )
    }
}