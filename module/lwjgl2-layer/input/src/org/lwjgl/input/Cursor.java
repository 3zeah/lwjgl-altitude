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

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Cursor {

    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;

    private final long[] elements;

    public Cursor(
            int width,
            int height,
            int xHotspot,
            int yHotspot,
            int imageCount,
            IntBuffer images,
            @SuppressWarnings("unused") IntBuffer delays // animation support not used by altitude
    ) throws LWJGLException {
        elements = new long[imageCount];

        IntBuffer axisCorrectImages = BufferUtils.createIntBuffer(images.limit());
        flipYAxisOfImages(width, height, imageCount, images, axisCorrectImages);
        // altitude sends argb, but unlike lwjgl2, glfw expects little-endian rgba
        ByteBuffer pixels = argbToLittleEndianRgba(width, height, axisCorrectImages);

        int byteCountOfSingleImage = width * height * 4;
        for (int i = 0; i < imageCount; i++) {
            ByteBuffer image = BufferUtils.createByteBuffer(byteCountOfSingleImage);
            for (int j = 0; j < byteCountOfSingleImage; j++) {
                image.put(pixels.get());
            }
            image.flip();

            try (GLFWImage cursorImage = GLFWImage.malloc()) {
                cursorImage.width(width);
                cursorImage.height(height);
                cursorImage.pixels(image);
                long cursor = GLFW.glfwCreateCursor(cursorImage, xHotspot, yHotspot);
                if (cursor == NULL) {
                    throw new LWJGLException("Failed to create GLFW cursor");
                }
                elements[i] = cursor;
            }
        }
    }

    public void destroy() {
        for (long cursor : elements) {
            glfwDestroyCursor(cursor);
        }
    }

    long handle() {
        // animation support not used by altitude
        return elements[0];
    }

    public static int getCapabilities() {
        // altitude checks this capability, and otherwise does not render the custom cursor, because lwjgl2 will refuse
        // to set a native cursor if this capability is not supported. although i am not sure whether this capability is
        // actually supported, or how it is queried in glfw, it is probably no longer relevant at all. note, eg, that
        // the image data is sent as 32-bit argb
        return CURSOR_ONE_BIT_TRANSPARENCY;
    }

    // not actually sure what the minimum is, but the altitude crosshair is 32x32 so whatever
    public static int getMinCursorSize() {
        return 1;
    }

    // not actually sure what the maximum is, but the altitude crosshair is 32x32 so whatever
    public static int getMaxCursorSize() {
        return 512;
    }

    private static void flipYAxisOfImages(int width, int height, int numImages, IntBuffer images, IntBuffer images_copy) {
        for (int i = 0; i < numImages; i++) {
            int start_index = i * width * height;
            flipYAxisOfImage(width, height, start_index, images, images_copy);
        }
    }

    private static void flipYAxisOfImage(int width, int height, int start_index, IntBuffer images, IntBuffer images_copy) {
        for (int y = 0; y < height >> 1; y++) {
            int index_y_1 = y * width + start_index;
            int index_y_2 = (height - y - 1) * width + start_index;
            for (int x = 0; x < width; x++) {
                int index1 = index_y_1 + x;
                int index2 = index_y_2 + x;
                int temp_pixel = images.get(index1 + images.position());
                images_copy.put(index1, images.get(index2 + images.position()));
                images_copy.put(index2, temp_pixel);
            }
        }
    }

    private static ByteBuffer argbToLittleEndianRgba(int width, int height, IntBuffer imageBuffer) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        for (int i = 0; i < imageBuffer.limit(); i++) {
            int argbColor = imageBuffer.get(i);

            int reversed = Integer.reverseBytes(argbColor);
            byte blue = (byte) (reversed >>> 24);
            byte green = (byte) (reversed >>> 16);
            byte red = (byte) (reversed >>> 8);
            byte alpha = (byte) reversed;

            pixels.put(red);
            pixels.put(green);
            pixels.put(blue);
            pixels.put(alpha);
        }
        pixels.flip();
        return pixels;
    }
}
