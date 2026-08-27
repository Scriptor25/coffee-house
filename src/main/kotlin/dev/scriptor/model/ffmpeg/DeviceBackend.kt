package dev.scriptor.model.ffmpeg

enum class DeviceBackend(val device: DeviceId, val priority: Int, val scale: FilterId?) {
    CUDA(DeviceId("cuda"), 1, FilterId("scale_cuda")),
    QSV(DeviceId("qsv"), 2, FilterId("vpp_qsv")),
    AMF(DeviceId("amf"), 3, FilterId("vpp_amf")),
    VAAPI(DeviceId("vaapi"), 4, FilterId("scale_vaapi")),
    VULKAN(DeviceId("vulkan"), 5, null);

    companion object {
        fun find(device: DeviceId): DeviceBackend? = entries.find { it.device == device }
    }
}
