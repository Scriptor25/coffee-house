package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import java.nio.file.Path
import kotlin.io.path.Path

class StringPathConverter : Converter<String, Path> {

    context(provider: Provider)
    override fun convert(value: String) = Path(value)
}
