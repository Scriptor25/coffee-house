package dev.scriptor.model.ffmpeg

enum class DeviceBackend(val device: DeviceId, val format: PixelFormat, val scale: FilterId?) {
    CUDA(DeviceId("cuda"), PixelFormat("cuda"), FilterId("scale_cuda")),
    VAAPI(DeviceId("vaapi"), PixelFormat("vaapi"), FilterId("scale_vaapi")),
    QSV(DeviceId("qsv"), PixelFormat("qsv"), FilterId("vpp_qsv")),
    AMF(DeviceId("amf"), PixelFormat("amf"), FilterId("vpp_amf")),
    VULKAN(DeviceId("vulkan"), PixelFormat("vulkan"), null),
}
