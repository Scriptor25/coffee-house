package dev.scriptor.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(val value: String, val join: Array<String> = [])
