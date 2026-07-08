package cn.advicenext.utility.minecraft.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.*;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Java 版 AWTUtils：将 GLFW 键鼠事件映射为 AWT/Compose 事件。
 * 注意：Compose 的 Key/KeyEvent 在 Java 中可能与 Kotlin 构造差异，若编译失败请根据实际 Compose API 调整 Key/KeyEvent 的构造方式。
 */
public final class AWTUtils {
    public static final Component awtComponent = new Component() { };

    private AWTUtils() { }

    public static int getAwtMods(long windowHandle) {
        int awtMods = 0;
        if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS)
            awtMods = awtMods | InputEvent.BUTTON1_DOWN_MASK;
        if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS)
            awtMods = awtMods | InputEvent.BUTTON2_DOWN_MASK;
        if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_3) == GLFW_PRESS)
            awtMods = awtMods | InputEvent.BUTTON3_DOWN_MASK;
        if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_4) == GLFW_PRESS)
            awtMods = awtMods | (1 << 14);
        if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_5) == GLFW_PRESS)
            awtMods = awtMods | (1 << 15);
        if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS)
            awtMods = awtMods | InputEvent.CTRL_DOWN_MASK;
        if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
            awtMods = awtMods | InputEvent.SHIFT_DOWN_MASK;
        if (glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS)
            awtMods = awtMods | InputEvent.ALT_DOWN_MASK;
        return awtMods;
    }

    public static int glfwToAwtButton(int glfwButton) {
        switch (glfwButton) {
            case GLFW_MOUSE_BUTTON_1: return MouseEvent.BUTTON1;
            case GLFW_MOUSE_BUTTON_2: return MouseEvent.BUTTON2;
            case GLFW_MOUSE_BUTTON_3: return MouseEvent.BUTTON3;
            default: return MouseEvent.BUTTON1;
        }
    }

    public static MouseEvent createMouseEvent(int mouseX, int mouseY, int awtMods, int button, int eventType) {
        return new MouseEvent(
            awtComponent,
            eventType,
            System.currentTimeMillis(),
            awtMods,
            mouseX,
            mouseY,
            1,
            false,
            glfwToAwtButton(button)
        );
    }

    public static MouseWheelEvent createMouseWheelEvent(int x, int y, double scrollY, int awtMods, int eventType) {
        return new MouseWheelEvent(
            awtComponent,
            eventType,
            System.currentTimeMillis(),
            awtMods,
            x,
            y,
            0,
            false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL,
            1,
            (int) (-scrollY)
        );
    }

    private static boolean isCtrlPressed(long windowHandle) {
        return glfwGetKey(windowHandle, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS;
    }

    private static boolean isShiftPressed(long windowHandle) {
        return glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
    }

    private static boolean isAltPressed(long windowHandle) {
        return glfwGetKey(windowHandle, GLFW_KEY_LEFT_ALT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_ALT) == GLFW_PRESS;
    }

    /**
     * 通过 Compose 的 KeyEvent 构造一个事件实例（Kotlin 版使用 @OptIn）。
     * 如果你的 Compose 版本在 Java 中无法直接构造 Key / KeyEvent，请根据实际 API 调整此处构造方式。
     */

}
