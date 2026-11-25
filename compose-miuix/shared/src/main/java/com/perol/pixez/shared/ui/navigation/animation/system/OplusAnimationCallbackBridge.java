package com.perol.pixez.shared.ui.navigation.animation.system;

import com.oplus.animation.OplusViewSeamless;
import java.util.function.BiConsumer;

/** 仅在确认系统回调父类存在后按类名加载。 */
public final class OplusAnimationCallbackBridge extends OplusViewSeamless.AnimationCallback {
    private final BiConsumer<String, Boolean> listener;

    private OplusAnimationCallbackBridge(BiConsumer<String, Boolean> listener) {
        this.listener = listener;
    }

    public static Object create(BiConsumer<String, Boolean> listener) {
        return new OplusAnimationCallbackBridge(listener);
    }

    @Override
    public void onAnimationStart(boolean isOpen) {
        listener.accept("start", isOpen);
    }

    @Override
    public void onAnimationEnd(boolean isOpen) {
        listener.accept("end", isOpen);
    }
}
