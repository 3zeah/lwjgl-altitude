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

import org.lwjgl.glfw.GLFWVidMode;

import java.util.Objects;

public final class DisplayMode {

    private final int width;
    private final int height;
    private final int bitsPerPixel;
    private final int frequency;
    private final boolean supportsFullscreen;

    public DisplayMode(int width, int height) {
        this(width, height, 0, 0, false);
    }

    private DisplayMode(int width, int height, int bitsPerPixel, int frequency) {
        this(width, height, bitsPerPixel, frequency, true);
    }

    private DisplayMode(int width, int height, int bitsPerPixel, int frequency, boolean supportsFullscreen) {
        this.width = width;
        this.height = height;
        this.bitsPerPixel = bitsPerPixel;
        this.frequency = frequency;
        this.supportsFullscreen = supportsFullscreen;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getFrequency() {
        return frequency;
    }

    boolean supportsFullscreen() {
        return supportsFullscreen;
    }

    public boolean sameSizeAs(DisplayMode other) {
        return this.width == other.width && this.height == other.height;
    }

    @Override
    public String toString() {
        return "%d x %d x %d @%dHz".formatted(width, height, bitsPerPixel, frequency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DisplayMode that = (DisplayMode) o;
        return width == that.width
                && height == that.height
                && bitsPerPixel == that.bitsPerPixel
                && frequency == that.frequency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, bitsPerPixel, frequency);
    }

    static DisplayMode adapt(GLFWVidMode vidMode) {
        return new DisplayMode(
                vidMode.width(),
                vidMode.height(),
                vidMode.redBits() + vidMode.greenBits() + vidMode.blueBits(),
                vidMode.refreshRate()
        );
    }
}
