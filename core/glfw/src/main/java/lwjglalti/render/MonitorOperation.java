package lwjglalti.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWGammaRamp;

import java.nio.ShortBuffer;

import static org.lwjgl.glfw.GLFW.glfwSetGammaRamp;

/**
 * Monitor operations that do not depend on the {@link org.lwjgl.opengl.Display} state, extracted to simplify reasoning
 * about the effect of that state
 */
public class MonitorOperation {

    private MonitorOperation() {
        // static api
    }

    // input gamma values are inverted for lwjgl2, and hence we cannot simply use `glfwSetGamma`
    public static void setGammaRampFromLwjgl2Gamma(long monitor, int size, float gamma) {
        ShortBuffer values = BufferUtils.createShortBuffer(size);
        for (int i = 0; i < size; ++i) {
            float intensity = (float) i / (size - 1);
            float value = (float) Math.pow(intensity, gamma);
            // normalize to short
            value = value * 0xFFFF + 0.5F;
            // clamp
            if (value > 0xFFFF) {
                value = 0xFFFF;
            } else if (value < 0F) {
                value = 0F;
            }
            values.put((short) value);
        }
        values.flip();
        try (GLFWGammaRamp result = GLFWGammaRamp.malloc()) {
            result.set(values, values, values, size);
            glfwSetGammaRamp(monitor, result);
        }
    }
}
