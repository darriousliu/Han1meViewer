package io.github.darriousliu.han1meviewer.feature.video.player

/**
 * 超分辨率（Anime4K）能力。只有 mpv 内核实现；
 * 控件层用 `controller is SuperResolutionController` 决定超分入口是否显示。
 */
interface SuperResolutionController {

    /** 0=关闭，1=效能优先，2=品质优先（与 `super_resolution_*` 选项同序）。 */
    fun setSuperResolution(index: Int)
}
