package dev.scriptor.converter

import dev.scriptor.realPathString
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import java.nio.file.Path

class PathStringConverter : Converter<Path, String> {

    context(provider: Provider)
    override fun convert(value: Path) = value.realPathString()
}
