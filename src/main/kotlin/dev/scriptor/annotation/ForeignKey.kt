package dev.scriptor.annotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForeignKey(val table: String, val column: String)
