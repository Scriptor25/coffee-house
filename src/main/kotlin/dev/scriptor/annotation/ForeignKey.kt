package dev.scriptor.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForeignKey(val type: KClass<*>, val name: String)
