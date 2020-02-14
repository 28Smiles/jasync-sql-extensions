package com.github.jasync_sql_extensions.mapper.asm

import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.MapperCreator
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.reflect.KClass

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
object AsmMapperCreator: MapperCreator {
    private val cache: LoadingCache<KClass<out Any>, Mapper<out Any>> =
            CacheBuilder.newBuilder()
                    .build(object : CacheLoader<KClass<out Any>, Mapper<out Any>>() {
                        override fun load(key: KClass<out Any>): Mapper<out Any> {
                            return MapperSynthesizer.synthesize(key)
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
    override fun <Bean: Any>get(clazz: KClass<Bean>): Mapper<Bean> = cache[clazz] as Mapper<Bean>

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