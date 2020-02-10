package com.github.jasync_sql_extensions.asm

/**
 * @author Leon Camus
 * @since 08.02.2020
 */
internal object DynamicClassLoader: ClassLoader() {
    fun defineClass(name: String?, b: ByteArray): Class<*>? {
        return this.defineClass(name, b, 0, b.size)
    }
}
