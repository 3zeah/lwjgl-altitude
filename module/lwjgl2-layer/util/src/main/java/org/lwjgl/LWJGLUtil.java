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
package org.lwjgl;

import java.security.PrivilegedAction;

// yes, AccessController is no longer OK, but for initial compatibility we use the LWJGL2 impls
@SuppressWarnings("removal")
public class LWJGLUtil {

    private LWJGLUtil() {
        // static api
    }

    static final int PLATFORM_LINUX = 1;
    static final int PLATFORM_MACOSX = 2;
    static final int PLATFORM_WINDOWS = 3;

    private static final int PLATFORM = readPlatform();

    public static int getPlatform() {
        return PLATFORM;
    }

    public static String getPlatformName() {
        return switch (LWJGLUtil.getPlatform()) {
            case PLATFORM_LINUX -> "linux";
            case PLATFORM_MACOSX -> "macosx";
            case PLATFORM_WINDOWS -> "windows";
            default -> "unknown";
        };
    }

    private static int readPlatform() {
        final String osName = java.security.AccessController.doPrivileged((PrivilegedAction<String>) () ->
                System.getProperty("os.name")
        );
        if (osName.startsWith("Windows")) {
            return PLATFORM_WINDOWS;
        } else if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("OpenBSD") || osName.startsWith("SunOS") || osName.startsWith("Unix")) {
            return PLATFORM_LINUX;
        } else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
            return PLATFORM_MACOSX;
        } else {
            throw new LinkageError("Unknown platform: " + osName);
        }
    }
}
