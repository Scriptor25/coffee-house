package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class PathStringConverter : Converter<Path, String> {

    context(provider: Provider)
    override fun convert(value: Path) = value.absolutePathString()
}
