package dev.scriptor

sealed interface Encoder {

    val codec: Codec

    operator fun invoke(
        index: Int,
        profile: Profile,
        bitrate: Long,
    ): List<String>

    data object Copy : Encoder {

        override val codec = Codec.COPY

        override operator fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = emptyList()
    }

    data object AV1 : Encoder {

        private fun preset(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 3
            Profile.HIGH -> 5
            Profile.MEDIUM -> 7
            Profile.LOW -> 9
            Profile.POTATO -> 11
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 25
            Profile.MEDIUM -> 30
            Profile.LOW -> 35
            Profile.POTATO -> 40
        }

        override val codec = Codec.AV1

        override operator fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile).toString(),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )
    }

    data object VP8 : Encoder {

        private fun cpuUsed(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 0
            Profile.HIGH -> 1
            Profile.MEDIUM -> 2
            Profile.LOW -> 4
            Profile.POTATO -> 6
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 24
            Profile.MEDIUM -> 30
            Profile.LOW -> 36
            Profile.POTATO -> 42
        }

        override val codec = Codec.VP8

        override operator fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-cpu-used:v:$index", cpuUsed(profile).toString(),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )
    }

    data object VP9 : Encoder {

        private fun cpuUsed(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 0
            Profile.HIGH -> 1
            Profile.MEDIUM -> 2
            Profile.LOW -> 4
            Profile.POTATO -> 6
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 24
            Profile.MEDIUM -> 30
            Profile.LOW -> 36
            Profile.POTATO -> 42
        }

        override val codec = Codec.VP9

        override operator fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-cpu-used:v:$index", cpuUsed(profile).toString(),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )
    }

    data object H264 : Encoder {

        private fun preset(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "veryslow"
            Profile.HIGH -> "slow"
            Profile.MEDIUM -> "medium"
            Profile.LOW -> "veryfast"
            Profile.POTATO -> "superfast"
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 20
            Profile.MEDIUM -> 22
            Profile.LOW -> 24
            Profile.POTATO -> 26
        }

        override val codec = Codec.H264

        override operator fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )
    }

    data object H265 : Encoder {

        private fun preset(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "veryslow"
            Profile.HIGH -> "slow"
            Profile.MEDIUM -> "medium"
            Profile.LOW -> "veryfast"
            Profile.POTATO -> "superfast"
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 24
            Profile.HIGH -> 26
            Profile.MEDIUM -> 28
            Profile.LOW -> 30
            Profile.POTATO -> 32
        }

        override val codec = Codec.H265

        override operator fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )
    }
}
