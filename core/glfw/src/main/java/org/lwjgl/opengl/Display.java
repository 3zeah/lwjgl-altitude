/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.opengl;

import lwjglalti.render.GammaRamp;
import lwjglalti.render.MonitorOperation;
import lwjglalti.render.Properties;
import lwjglalti.render.WindowOperation;
import lwjglalti.render.WindowOperation.WindowDefinition;
import org.lwjgl.LWJGLException;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWGammaRamp;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static lwjglalti.util.LwjglAltitudeUtil.freeIfPresent;
import static org.lwjgl.glfw.GLFW.GLFW_CENTER_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_DEPTH_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_STENCIL_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetGammaRamp;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetVideoModes;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetGammaRamp;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIconifyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Display {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // altitude relies on static display init: eg, altitude queries the monitor mode statically
    static {
        glfwSetErrorCallback(errorLogger());
        boolean initSuccess = glfwInit();
        if (!initSuccess) {
            throw new IllegalStateException("GLFW init failed");
        }
    }

    // there is probably no good reason to cache the monitor mode this way, and will probably lead to issues if the
    // user changes resolution while running altitude, but unfortunately altitude will statically cache it regardless
    private static final long INITIAL_PRIMARY_MONITOR = glfwGetPrimaryMonitor();
    private static final DisplayMode INITIAL_PRIMARY_MONITOR_DISPLAY_MODE =
            DisplayMode.adapt(glfwGetVideoMode(INITIAL_PRIMARY_MONITOR));
    private static final GammaRamp INITIAL_GAMMA_RAMP = GammaRamp.createFrom(glfwGetGammaRamp(INITIAL_PRIMARY_MONITOR));

    private static long window = NULL;
    private static boolean altitudeWantsToRecreateDisplay = false;

    // FIELD GROUP: monitor state
    private static Float gamma = null;

    // FIELD GROUP: window state that may be set before window is created, and thus needs to be retained here
    private static DisplayMode displayMode = null;
    private static WindowMode windowMode = null;
    // whether actually fullscreen depends also on whether the display mode allows it
    private static boolean exclusiveFullscreenIsDesired = false;
    private static Boolean vsync = null;
    private static String title = "";
    private static ByteBuffer[] icons = null;

    // FIELD GROUP: window-callback state
    private static boolean iconified = false;
    private static boolean focused = false;

    private Display() {
        // static api
    }

    // these are misnomers, since glfw does not recognize different fullscreen variants, but they are the conventional
    // names for what we are attempting to achieve
    public enum WindowMode {
        EXCLUSIVE_FULLSCREEN,
        WINDOWED_FULLSCREEN,
        WINDOWED,
    }

    // CREATION

    public static void create() throws LWJGLException {
        // this defaults to 0 alpha, 8 depth, and 0 stencil in lwjgl2, but is used in altitude to leave the pixel format
        // unspecified, and altitude does not actually use these buffers: thus, letting glfw decide should be more
        // robust
        create(null);
    }

    @SuppressWarnings("unused") // signature must match lwjgl 2 api
    public static void create(PixelFormat pixelFormat) throws LWJGLException {
        if (windowIsCreated()) {
            return;
        }
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        if (pixelFormat != null) {
            glfwWindowHint(GLFW_DEPTH_BITS, pixelFormat.depthBits());
            glfwWindowHint(GLFW_STENCIL_BITS, pixelFormat.stencilBits());
        }
        glfwWindowHint(GLFW_CENTER_CURSOR, GLFW_FALSE);

        WindowDefinition definition = WindowOperation.windowDefinition(
                INITIAL_PRIMARY_MONITOR,
                INITIAL_PRIMARY_MONITOR_DISPLAY_MODE,
                displayMode,
                windowMode
        );
        Display.window = WindowOperation.createWindow(title, definition);
        setWindowCallbacks();
        if (icons != null) {
            WindowOperation.setWindowIcons(window, icons);
        }
        triggerUpdatesAfterModeChange();
        glfwMakeContextCurrent(window);
        if (vsync != null) {
            WindowOperation.setVsync(vsync);
        }
        GL.createCapabilities();
        glfwShowWindow(window);
        focused = true;
    }

    private static void triggerUpdatesAfterModeChange() {
        WindowOperation.updateFloating(window, windowMode, focused);
        Mouse.setCapturedByDisplay(Objects.equals(windowMode, WindowMode.EXCLUSIVE_FULLSCREEN));
        updateGamma();
    }

    public static void destroy() {
        // see method `isCreated` for a rationale, but, yes, this is a flagrant hack
        if (altitudeWantsToRecreateDisplay) {
            altitudeWantsToRecreateDisplay = false;
            return;
        }
        if (windowIsCreated()) {
            Callbacks.glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            window = NULL;
        }
        glfwTerminate();
        freeIfPresent(glfwSetErrorCallback(null));
    }

    public static boolean isCreated() {
        boolean isCreated = windowIsCreated();
        // this is a major hack to prevent altitude from forcibly recreating the window on every display-mode change:
        // we know that this method is only used when changing the display mode to check whether to destroy the window
        // before applying the display-mode changes. thus, after a call to this method, we can ignore the next call to
        // destroy. there may be a good reason to recreate the window, in which case we will have to adapt, but for now
        // i assume that altitude does this for bullshit technical limitations of lwjgl2
        if (isCreated) {
            // `destroy` should have consumed this, if the assumption above is correct
            if (altitudeWantsToRecreateDisplay) {
                LOG.error(
                        "Unexpected display interfacing: Altitude asked whether display was created without " +
                                "destroying it after"
                );
            }
            altitudeWantsToRecreateDisplay = true;
        }
        return isCreated;
    }

    // these are set only once after creating the window, so there are no previous callbacks
    @SuppressWarnings("resource")
    private static void setWindowCallbacks() {
        glfwSetWindowIconifyCallback(window, (__, iconified) ->
                Display.iconified = iconified
        );
        glfwSetWindowFocusCallback(window, (__, focused) ->
                setFocused(focused)
        );
        glfwSetKeyCallback(window, (__, key, scancode, action, mods) ->
                Keyboard.registerGlfwKeyEvent(key, action, mods)
        );
        glfwSetCharCallback(window, (__, codepoint) ->
                Keyboard.registerGlfwCharEvent(codepoint)
        );
        glfwSetMouseButtonCallback(window, (__, button, action, mods) ->
                Mouse.registerGlfwMouseButtonEvent(button, action)
        );
        glfwSetCursorPosCallback(window, (__, xpos, ypos) ->
                Mouse.registerGlfwCursorPositionEvent(xpos, ypos)
        );
        glfwSetScrollCallback(window, (__, xoffset, yoffset) ->
                Mouse.registerGlfwScrollEvent(yoffset)
        );
    }

    private static GLFWErrorCallbackI errorLogger() {
        return (error, description) -> {
            String message = GLFWErrorCallback.getDescription(description);
            LOG.error("GLFW error {}: {}", error, message);
        };
    }

    // WINDOW CONFIGURATION

    public static void setTitle(String title) {
        if (title.equals(Display.title)) {
            return;
        }
        Display.title = title;
        if (windowIsCreated()) {
            WindowOperation.setWindowTitle(window, title);
        }
    }

    @SuppressWarnings("UnusedReturnValue") // this has to match lwjgl2 api signature
    public static int setIcon(ByteBuffer[] icons) {
        if (Arrays.equals(icons, Display.icons)) {
            return 1;
        }
        Display.icons = icons;
        if (windowIsCreated()) {
            WindowOperation.setWindowIcons(window, icons);
        }
        return 1; // return value never read
    }

    public static void setLocation(int x, int y) {
        // only called after initializing the frame: skip specially handling whether window exists
        glfwSetWindowPos(window, x, y);
    }

    // DISPLAY MODE

    @SuppressWarnings("RedundantThrows") // lwjgl2 api signature retained for posterity
    public static void setDisplayMode(DisplayMode mode) throws LWJGLException {
        DisplayMode oldDisplayMode = Display.displayMode;
        WindowMode oldWindowMode = Display.windowMode;
        Display.displayMode = mode;
        // display mode is always set after fullscreen is set: it is ok to evaluate the window mode now, which depends
        // on both
        Display.windowMode = evaluateWindowMode();
        boolean modeChanged = !Objects.equals(oldDisplayMode, Display.displayMode)
                || !Objects.equals(oldWindowMode, Display.windowMode);
        if (windowIsCreated() && modeChanged) {
            WindowDefinition definition = WindowOperation.windowDefinition(
                    INITIAL_PRIMARY_MONITOR,
                    INITIAL_PRIMARY_MONITOR_DISPLAY_MODE,
                    displayMode,
                    windowMode
            );
            WindowOperation.updateWindow(window, definition);
            // workaround for https://github.com/glfw/glfw/issues/1163: icon must be set while windowed for title bar
            if (Display.windowMode == WindowMode.WINDOWED && icons != null) {
                WindowOperation.setWindowIcons(window, icons);
            }
            triggerUpdatesAfterModeChange();
        }
    }

    private static WindowMode evaluateWindowMode() {
        DisplayMode monitor = getDesktopDisplayMode();
        WindowMode result = evaluateWindowModeIgnoringProperties(monitor);
        if (Properties.preferWindowedFullscreen()
                && result == WindowMode.EXCLUSIVE_FULLSCREEN
                && Display.displayMode.sameSizeAs(monitor)) {
            return WindowMode.WINDOWED_FULLSCREEN;
        } else {
            return result;
        }
    }

    private static WindowMode evaluateWindowModeIgnoringProperties(DisplayMode monitor) {
        if (exclusiveFullscreenIsDesired && displayMode.supportsFullscreen()) {
            return WindowMode.EXCLUSIVE_FULLSCREEN;
        } else if (Display.displayMode.sameSizeAs(monitor)) {
            return WindowMode.WINDOWED_FULLSCREEN;
        } else {
            return WindowMode.WINDOWED;
        }
    }

    // the complete window mode depends on both the display mode and whether fullscreen is desired, but the display
    // mode is always set after fullscreen is set here, so do nothing here
    @SuppressWarnings("RedundantThrows") // lwjgl2 api signature retained for posterity
    public static void setFullscreen(boolean fullscreen) throws LWJGLException {
        Display.exclusiveFullscreenIsDesired = fullscreen;
    }

    public static void setVSyncEnabled(boolean vsync) {
        if (Objects.equals(vsync, Display.vsync)) {
            return;
        }
        Display.vsync = vsync;
        if (windowIsCreated()) {
            WindowOperation.setVsync(vsync);
        }
    }

    @SuppressWarnings({"unused", "RedundantThrows"}) // lwjgl2 api signature retained for posterity
    public static void setDisplayConfiguration(float gamma, float brightness, float contrast) throws LWJGLException {
        // altitude will always pass brightness = 0 and contrast = 1, which in lwjgl2 is a no-op: consider only gamma
        if (Objects.equals(gamma, Display.gamma)) {
            return;
        }
        Display.gamma = gamma;
        updateGamma();
    }

    private static void updateGamma() {
        // if `INITIAL_GAMMA_RAMP` is null, getting the gamma ramp failed, and the gamma system therefore is unsupported
        if (INITIAL_GAMMA_RAMP == null || Display.gamma == null) {
            return;
        }
        // this condition is whether altitude considers the current mode to be exclusive fullscreen. we could support
        // gamma correction always, but because altitude will not forward updated gamma values unless `isFullscreen`,
        // we have to match altitude
        if (Display.focused && isFullscreen()) {
            MonitorOperation
                    .setGammaRampFromLwjgl2Gamma(INITIAL_PRIMARY_MONITOR, INITIAL_GAMMA_RAMP.size(), Display.gamma);
        } else {
            try (GLFWGammaRamp ramp = INITIAL_GAMMA_RAMP.allocateAsGlfwRamp()) {
                glfwSetGammaRamp(INITIAL_PRIMARY_MONITOR, ramp);
            }
        }
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Whether Altitude considers the current mode to be exclusive fullscreen. Not necessarily the same as the actual
     * window mode.
     */
    public static boolean isFullscreen() {
        // we support overriding or changing the display mode via properties, but that cannot affect how we communicate
        // with altitude, since the result here must match with what altitude expects from its actual settings
        WindowMode currentModeAccordingToAltitude = evaluateWindowModeIgnoringProperties(getDesktopDisplayMode());
        return currentModeAccordingToAltitude == WindowMode.EXCLUSIVE_FULLSCREEN;
    }

    // CAPABILITIES

    public static DisplayMode getDesktopDisplayMode() {
        return INITIAL_PRIMARY_MONITOR_DISPLAY_MODE;
    }

    public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        GLFWVidMode.Buffer vidModes = glfwGetVideoModes(INITIAL_PRIMARY_MONITOR);
        if (vidModes == null) {
            throw new LWJGLException("Failed to get available display modes");
        }
        DisplayMode[] result = new DisplayMode[vidModes.limit()];
        int i = 0;
        for (GLFWVidMode vidMode : vidModes) {
            result[i++] = DisplayMode.adapt(vidMode);
        }
        return result;
    }

    // STATE

    public static void processMessages() {
        glfwPollEvents();
    }

    @SuppressWarnings("RedundantThrows") // lwjgl2 api signature retained for posterity
    public static void swapBuffers() throws LWJGLException {
        glfwSwapBuffers(window);
    }

    public static boolean isCloseRequested() {
        return glfwWindowShouldClose(window);
    }

    public static boolean isVisible() {
        return !iconified;
    }

    public static boolean isActive() {
        return focused;
    }

    private static void setFocused(boolean focused) {
        Display.focused = focused;
        WindowOperation.updateFloating(Display.window, windowMode, focused);
        updateGamma();
    }

    // NOT DIRECTLY CALLED BY ALTITUDE

    public static long window() {
        return window;
    }

    public static boolean windowIsCreated() {
        return window != NULL;
    }

    public static DisplayMode displayMode() {
        return displayMode;
    }
}
