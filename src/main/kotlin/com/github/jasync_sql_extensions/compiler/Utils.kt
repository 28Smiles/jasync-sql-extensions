package com.github.jasync_sql_extensions.compiler

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.function.Function
import javax.tools.DiagnosticCollector
import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType

val javaCompiler: JavaCompiler? = ToolProvider.getSystemJavaCompiler()

val loading = ConcurrentHashMap<KFunction<Any>, Boolean>()
val lambdaCache = ConcurrentHashMap<KFunction<Any>, Function<Array<Any?>, Any>>()
val executor = Executors.newSingleThreadExecutor()

inline fun <reified Bean : Any>compile(constructor: KFunction<Any>): Function<Array<Any?>, Any> {
    val className = "com.github.jasync_sql_extensions.generated.BeanSupplier$${Bean::class.simpleName}"
    val source = """
        package com.github.jasync_sql_extensions.generated;
        
        public final class BeanSupplier$${Bean::class.simpleName} 
            implements java.util.function.Function<java.lang.Object[], ${Bean::class.qualifiedName}> {
            
            public ${Bean::class.qualifiedName} apply(java.lang.Object[] args) {
                return new ${Bean::class.qualifiedName}(
                    ${constructor.parameters.mapIndexed { index, parameter ->
                        "(${parameter.type.javaType.typeName}) args[$index]"
                    }.joinToString(",")}
                );
            }
        }
    """

    val fileObject = StringGeneratedSourceFileObject(className, source)
    val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
    val classLoader = StringGeneratedClassLoader(ClassLoader.getSystemClassLoader())
    val standardFileManager = javaCompiler!!.getStandardFileManager(diagnosticCollector, null, null)

    val javaFileManager = StringGeneratedJavaFileManager(standardFileManager, classLoader)
    val task = javaCompiler.getTask(
            null,
            javaFileManager,
            diagnosticCollector,
            null,
            null,
            Collections.singletonList(fileObject)
    )
    if (task.call()) {
        @Suppress("UNCHECKED_CAST") val clazz = classLoader.loadClass(className) as Class<Function<Array<Any?>, Any>>
        return clazz.getConstructor().newInstance()
    }
    return Function { t -> constructor.call(*t) }
}

inline fun <reified Bean : Any> createCompiledSupplierOrFallback(constructor: KFunction<Bean>): Function<Array<Any?>, Bean> {
    if (javaCompiler != null) {
        val lambda = lambdaCache[constructor]
        if (lambda != null) {
            return lambda as Function<Array<Any?>, Bean>
        }

        if (lambdaCache.containsKey(constructor)) {
            return Function { t -> constructor.call(*t) }
        }

        return synchronized(lambdaCache) {
            if (loading.containsKey(constructor)) {
                Function { t -> constructor.call(*t) }
            } else {
                loading[constructor] = true
                executor.submit {
                    lambdaCache[constructor] = compile<Bean>(constructor)
                }
                Function { t -> constructor.call(*t) }
            }
        }
    } else {
        return Function { t -> constructor.call(*t) }
    }
}
