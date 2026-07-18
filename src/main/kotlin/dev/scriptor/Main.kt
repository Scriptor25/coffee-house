package dev.scriptor

import dev.scriptor.server.scan

fun main() {
    scan(8080, false, packageName = "dev.scriptor").use { it.start() }
}
