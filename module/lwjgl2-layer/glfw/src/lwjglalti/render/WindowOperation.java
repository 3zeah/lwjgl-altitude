package lwjglalti.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.Display.WindowMode;
import org.lwjgl.opengl.DisplayMode;

import java.nio.ByteBuffer;

import static lwjglalti.util.LwjglAltitudeUtil.glfwBoolean;
import static org.lwjgl.glfw.GLFW.GLFW_AUTO_ICONIFY;
import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_FLOATING;
import static org.lwjgl.glfw.GLFW.GLFW_POSITION_X;
import static org.lwjgl.glfw.GLFW.GLFW_POSITION_Y;
import static org.lwjgl.glfw.GLFW.GLFW_REFRESH_RATE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwSetWindowAttrib;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.Display.WindowMode.EXCLUSIVE_FULLSCREEN;
import static org.lwjgl.opengl.Display.WindowMode.WINDOWED;
import static org.lwjgl.opengl.Display.WindowMode.WINDOWED_FULLSCREEN;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Window operations that do not depend on the {@link org.lwjgl.opengl.Display} state, extracted to simplify reasoning
 * about the effect of that state
 */
public class WindowOperation {

    private WindowOperation() {
        // static api
    }

    public static long createWindow(String title, WindowDefinition definition)
            throws LWJGLException {
        glfwWindowHint(GLFW_DECORATED, glfwBoolean(definition.decorated()));
        glfwWindowHint(GLFW_AUTO_ICONIFY, glfwBoolean(definition.iconify()));
        glfwWindowHint(GLFW_POSITION_X, definition.x());
        glfwWindowHint(GLFW_POSITION_Y, definition.y());
        glfwWindowHint(GLFW_REFRESH_RATE, definition.refreshRate());
        long window = glfwCreateWindow(definition.width(), definition.height(), title, definition.monitor(), NULL);
        if (window == NULL) {
            throw new LWJGLException("GLFW window creation failed");
        }
        return window;
    }

    public static void updateWindow(long window, WindowDefinition definition) {
        glfwSetWindowAttrib(window, GLFW_AUTO_ICONIFY, glfwBoolean(definition.iconify()));
        glfwSetWindowAttrib(window, GLFW_DECORATED, glfwBoolean(definition.decorated()));
        glfwSetWindowMonitor(
                window,
                definition.monitor(),
                definition.x(),
                definition.y(),
                definition.width(),
                definition.height(),
                definition.refreshRate()
        );
    }

    public static WindowDefinition windowDefinition(
            long monitor,
            DisplayMode monitorDisplayMode,
            DisplayMode displayMode,
            WindowMode windowMode
    ) {
        long monitorOrNull = windowMode == EXCLUSIVE_FULLSCREEN ? monitor : NULL;
        boolean iconify = windowMode == EXCLUSIVE_FULLSCREEN;
        boolean decorated = windowMode != WINDOWED_FULLSCREEN && decoratedIsEnabled();
        final int x;
        final int y;
        if (windowMode == WINDOWED) {
            x = (monitorDisplayMode.getWidth() - displayMode.getWidth()) / 2;
            y = (monitorDisplayMode.getHeight() - displayMode.getHeight()) / 2;
        } else {
            x = 0;
            y = 0;
        }
        return new WindowDefinition(
                x,
                y,
                displayMode.getWidth(),
                displayMode.getHeight(),
                monitorOrNull,
                displayMode.getFrequency(),
                iconify,
                decorated
        );
    }

    public record WindowDefinition(
            int x,
            int y,
            int width,
            int height,
            long monitor,
            int refreshRate,
            boolean iconify,
            boolean decorated
    ) {

    }

    // this is how we simulate "windowed fullscreen": an undecorated window the size of the monitor, always on top if
    // focused
    //
    // glfw only has one "fullscreen". although glfw is smart enough not to trigger a video-mode change if not required
    // - ie when the fullscreen mode is the same as the monitor mode - the fullscreen mode of glfw is nonetheless a
    // bastardization of exclusive fullscreen in that it insists on remaining on top unless iconified
    public static void updateFloating(long window, WindowMode windowMode, boolean focused) {
        boolean floating = windowMode == WINDOWED_FULLSCREEN && focused;
        glfwSetWindowAttrib(window, GLFW_FLOATING, glfwBoolean(floating));
    }

    public static boolean decoratedIsEnabled() {
        return !Boolean.getBoolean("org.lwjgl.opengl.Window.undecorated");
    }

    public static void setVsync(boolean enabled) {
        glfwSwapInterval(enabled ? 1 : 0);
    }

    public static void setWindowTitle(long window, String title) {
        glfwSetWindowTitle(window, title);
    }

    public static void setWindowIcons(long window, ByteBuffer[] icons) {
        try (GLFWImage.Buffer images = GLFWImage.malloc(icons.length)) {
            for (ByteBuffer icon : icons) {
                ByteBuffer pixels = ensureAllocatedDirectly(icon);
                int dimension = (int) Math.sqrt(pixels.limit() / 4);
                try (GLFWImage image = GLFWImage.malloc()) {
                    image.width(dimension);
                    image.height(dimension);
                    image.pixels(pixels);
                    images.put(image);
                }
            }
            if (images.address() == NULL) {
                throw new IllegalStateException("Failed to set window-icon images");
            }
            glfwSetWindowIcon(window, images.flip());
        }
    }

    private static ByteBuffer ensureAllocatedDirectly(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            return buffer;
        }
        ByteBuffer result = BufferUtils.createByteBuffer(buffer.limit());
        buffer.rewind();
        result.put(buffer);
        result.flip();
        return result;
    }
}
