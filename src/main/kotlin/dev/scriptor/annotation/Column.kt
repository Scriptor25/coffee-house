package dev.scriptor.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val value: String = "", val type: KClass<*> = Unit::class, val table: String = "")
