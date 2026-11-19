package com.perol.pixez.shared.ui.coui.component

/**
 * COUI 库的 API 能力门槛，集中一处便于升级时复核。
 *
 * 依据来自 AAR 字节码里的版本判断：`CouiDialogDefaults` 在 33 以上才提供凝光材质，
 * `ChipTabRow` 与 `NavigationBar` 需要 34。低于门槛时本工程自绘同版式平替，
 * 因此这些常量只决定「走库还是走自绘」，不改变页面语义。
 */

/** `Dialog` 凝光材质门槛（API 33）。 */
internal const val COUI_DIALOG_MATERIAL_MIN_SDK = 33

/** `ChipTabRow` 胶囊筛选栏门槛（API 34）。 */
internal const val COUI_CHIP_TAB_MIN_SDK = 34

/** `NavigationBar` 浮动底栏门槛（API 34）。 */
internal const val COUI_LIBRARY_NAVIGATION_BAR_MIN_SDK = 34

/** `MessageInputBar` 与 `CouiFloatingRenderer.standalone()` 门槛（API 34）。 */
internal const val COUI_MESSAGE_INPUT_MIN_SDK = 34
