package dev.scriptor.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val unique: String = "",
)
