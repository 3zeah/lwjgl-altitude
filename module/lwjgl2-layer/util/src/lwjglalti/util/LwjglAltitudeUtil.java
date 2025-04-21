package lwjglalti.util;

import org.lwjgl.system.NativeResource;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;

public class LwjglAltitudeUtil {

    private LwjglAltitudeUtil() {
        // static api
    }

    public static int glfwBoolean(boolean value) {
        return value ? GLFW_TRUE : GLFW_FALSE;
    }

    public static void freeIfPresent(NativeResource resource) {
        if (resource == null) {
            return;
        }
        resource.close();
    }
}
