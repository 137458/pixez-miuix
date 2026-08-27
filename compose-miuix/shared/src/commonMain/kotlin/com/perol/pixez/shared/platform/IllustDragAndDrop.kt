package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.Illust

/**
 * 跨应用内容拖拽源扩展：支持在分屏、自由窗口下跨应用拖拽作品，并与系统/厂商内容中转站与传送门联动。
 *
 * - Android：构建含标准作品 URL 与标题的 ClipData，携带 DRAG_FLAG_GLOBAL 启动系统全局拖拽，
 *   支持 OPPO 智慧中转站/流体云/传送门、小米传送门、华为超级中转站及外部应用（如微信、备忘录）捕获。
 * - 其他平台：安全回退（保持原修饰符）。
 */
@Composable
expect fun Modifier.illustDragAndDropSource(illust: Illust): Modifier
