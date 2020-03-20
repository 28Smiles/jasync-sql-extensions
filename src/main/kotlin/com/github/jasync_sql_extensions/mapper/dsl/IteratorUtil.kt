package com.github.jasync_sql_extensions.mapper.dsl

fun <A : Any, B : Any> iteratorToMapper(mapper: (Iterator<A>) -> Iterator<B>): (A) -> B = object : (A) -> B {
    val iteratorIn = object : Iterator<A> {
        var element: A? = null

        override fun next(): A {
            val out = element!!
            element = null

            return out
        }

        override fun hasNext(): Boolean = element != null
    }
    val iteratorOut = mapper(iteratorIn)

    override fun invoke(p1: A): B {
        iteratorIn.element = p1
        return iteratorOut.next()
    }
}

fun <A, B> Iterator<A>.map(mapper: (A) -> B): Iterator<B> = object : Iterator<B> {
    override fun hasNext(): Boolean = this@map.hasNext()
    override fun next(): B = mapper(this@map.next())
}
