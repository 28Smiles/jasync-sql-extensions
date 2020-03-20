package com.github.jasync_sql_extensions.asm

internal object DynamicClassLoader: ClassLoader() {
    fun defineClass(name: String?, b: ByteArray): Class<*>? {
        return this.defineClass(name, b, 0, b.size)
    }
}
