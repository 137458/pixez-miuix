package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.Illust

/**
 * 跨应用内容拖拽源扩展：支持在分屏、自由窗口下跨应用拖拽作品，并与系统/厂商内容中转站与传送门联动。
 *
 * - Android：构建含标准原图/作品文件 Content URI、MIME 类型及作品 URL 的 ClipData，携带 DRAG_FLAG_GLOBAL
 *   与 DRAG_FLAG_GLOBAL_URI_READ 启动系统全局拖拽，支持原图无损直接提取，
 *   深度兼容小米传送门/超级岛、OPPO 智慧中转站/流体云/传送门、vivo 原子中转站/智慧识屏、荣耀任意门、华为超级中转站及外部应用。
 * - 其他平台：安全回退（保持原修饰符）。
 *
 * @param illust 目标插画作品实体
 * @param pageIndex 目标分页索引，单页作品传 0
 */
@Composable
expect fun Modifier.illustDragAndDropSource(illust: Illust, pageIndex: Int = 0): Modifier

