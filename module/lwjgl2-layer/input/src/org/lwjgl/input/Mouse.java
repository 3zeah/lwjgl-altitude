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
package org.lwjgl.input;

import org.lwjgl.LWJGLException;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.Display;

import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

public class Mouse {

    private static Integer cursorState = null;

    private static boolean isDisabled = false;
    private static boolean isCapturedByDisplay = false;
    private static boolean isMouseAiming = false;

    private static Cursor nativeCursor = null;

    private Mouse() {
        // static api
    }

    public static boolean isCreated() {
        return Display.windowIsCreated();
    }

    public static void poll() {
        // polling handled by display
    }

    public static boolean next() {
        // TODO impl events
        return false;
    }

    public static int getEventButton() {
        return 0;
    }

    public static int getEventX() {
        return 0;
    }

    public static int getEventY() {
        return 0;
    }

    public static int getEventDWheel() {
        return 0;
    }

    public static boolean getEventButtonState() {
        return false;
    }

    public static void setCursorPosition(int x, int y) {
        // glfw input position here is relative top left
        int flippedYPosition = Display.getDesktopDisplayMode().getHeight() - y;
        // assume window is created
        GLFW.glfwSetCursorPos(Display.window(), x, flippedYPosition);
    }

    public static void updateCursor() {
        // DO NOTHING: altitude was never supposed to, and never needed to, call this
    }

    public static Cursor getNativeCursor() {
        return nativeCursor;
    }

    // must match lwjgl 2 api signature (throws declaration included for posterity)
    @SuppressWarnings({"unused", "RedundantThrows"})
    public static Cursor setNativeCursor(Cursor cursor) throws LWJGLException {
        long window = Display.window();
        // assume window is created
        if (cursor == null) {
            isMouseAiming = false;
            GLFW.glfwSetCursor(window, NULL);
        } else {
            // this is a hack, but we happen to know that altitude will override the cursor image exactly when the user
            // is mouse aiming, to render the crosshair, and we need this information to know whether to lock the cursor
            // (this is not required for compatibility, but is rather a no-brainer bug fix)
            isMouseAiming = true;
            GLFW.glfwSetCursor(window, cursor.handle());
        }
        nativeCursor = cursor;
        updateCursorState();
        return cursor;
    }

    public static boolean isGrabbed() {
        return isDisabled;
    }

    public static void setGrabbed(boolean grab) {
        Mouse.isDisabled = grab;
        updateCursorState();
    }

    private static void updateCursorState() {
        final int state;
        if (isDisabled) {
            state = GLFW.GLFW_CURSOR_DISABLED;
        } else if (isCapturedByDisplay || isMouseAiming) {
            state = GLFW.GLFW_CURSOR_CAPTURED;
        } else {
            state = GLFW.GLFW_CURSOR_NORMAL;
        }
        if (Objects.equals(state, Mouse.cursorState)) {
            return;
        }
        Mouse.cursorState = state;
        // assume window is created
        GLFW.glfwSetInputMode(Display.window(), GLFW.GLFW_CURSOR, state);
    }

    // NOT DIRECTLY CALLED BY ALTITUDE

    public static void setCapturedByDisplay(boolean captured) {
        isCapturedByDisplay = captured;
        updateCursorState();
    }
}
