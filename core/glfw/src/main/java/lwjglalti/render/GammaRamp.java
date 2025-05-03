package lwjglalti.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWGammaRamp;

import java.nio.ShortBuffer;

/**
 * GLFW-style gamma ramp as plain old data. Use this for long-term storage over {@link GLFWGammaRamp}, because the
 * garbage collector might free the underlying buffers to which the represented native struct points.
 */
public class GammaRamp {

    private final int size;
    private final short[] red;
    private final short[] green;
    private final short[] blue;

    private GammaRamp(int size) {
        this.size = size;
        red = new short[size];
        green = new short[size];
        blue = new short[size];
    }

    public int size() {
        return size;
    }

    public GLFWGammaRamp allocateAsGlfwRamp() {
        try (GLFWGammaRamp result = GLFWGammaRamp.malloc()) {
            ShortBuffer redBuffer = BufferUtils.createShortBuffer(size).put(red).flip();
            ShortBuffer greenBuffer = BufferUtils.createShortBuffer(size).put(green).flip();
            ShortBuffer blueBuffer = BufferUtils.createShortBuffer(size).put(blue).flip();
            return result.set(redBuffer, greenBuffer, blueBuffer, size);
        }
    }

    public static GammaRamp createFrom(GLFWGammaRamp ramp) {
        if (ramp == null) {
            return null;
        }
        GammaRamp result = new GammaRamp(ramp.size());
        ramp.red().get(result.red);
        ramp.green().get(result.green);
        ramp.blue().get(result.blue);
        return result;
    }
}
