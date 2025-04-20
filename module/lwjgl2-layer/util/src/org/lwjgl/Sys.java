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

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static org.lwjgl.LWJGLUtil.PLATFORM_LINUX;
import static org.lwjgl.LWJGLUtil.PLATFORM_MACOSX;
import static org.lwjgl.LWJGLUtil.PLATFORM_WINDOWS;

// yes, AccessController is no longer OK, but for initial compatibility we use the LWJGL2 impls
@SuppressWarnings("removal")
public class Sys {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Sys() {
        // static api
    }

    public static void initialize() {
        // nothing to init yet
    }

    public static long getTimerResolution() {
        return GLFW.glfwGetTimerFrequency();
    }

    public static long getTime() {
        return GLFW.glfwGetTimerValue();
    }

    public static boolean openURL(String url) {
        boolean success = openUrlWithJava(url);
        if (success) {
            return true;
        }
        return openUrlWithLwjgl2Hacks(url);
    }

    private static boolean openUrlWithJava(String url) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return false;
        }
        try {
            desktop.browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to open URL using {}: {}", Desktop.class.getName(), url, e);
            return false;
        }
        return true;
    }

    // retained for compatibility: this is the old `openURL` method of lwjgl2
    private static boolean openUrlWithLwjgl2Hacks(String url) {
        try {
            return openUrlUsingWebStart(url);
        } catch (Exception e) {
            LOG.error("Failed to open URL using web start: {}", url, e);
            return switch (LWJGLUtil.getPlatform()) {
                case PLATFORM_LINUX -> openUrlForLinux(url);
                // there was a mac-specific implementation in lwjgl2, but it relied on some old apple extension
                // (com.apple.eio.FileManager) and this is nowhere near important enough to bother
                case PLATFORM_MACOSX -> false;
                case PLATFORM_WINDOWS -> openUrlForWindows(url);
                default -> throw new IllegalStateException("Unsupported platform");
            };
        }
    }

    private static boolean openUrlUsingWebStart(String url)
            throws ClassNotFoundException, PrivilegedActionException, InvocationTargetException, IllegalAccessException {
        Class<?> serviceManagerClass = Class.forName("javax.jnlp.ServiceManager");
        Method lookupMethod = java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () ->
                serviceManagerClass.getMethod("lookup", String.class)
        );
        Object basicService = lookupMethod.invoke(serviceManagerClass, "javax.jnlp.BasicService");
        Class<?> basicServiceClass = Class.forName("javax.jnlp.BasicService");
        Method showDocumentMethod = java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Method>) () ->
                basicServiceClass.getMethod("showDocument", URL.class)
        );
        final URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            LOG.error("Cannot open malformed URL: {}", url, e);
            return false;
        }
        return (Boolean) showDocumentMethod.invoke(basicService, parsedUrl);
    }

    private static boolean openUrlForLinux(String url) {
        String[] browsers = {"sensible-browser", "xdg-open", "google-chrome", "chromium", "firefox", "iceweasel", "mozilla", "opera", "konqueror", "nautilus", "galeon", "netscape"};
        for (final String browser : browsers) {
            try {
                execPrivileged(new String[]{browser, url});
                return true;
            } catch (Exception e) {
                LOG.error("Failed to open URL for Linux: {}", url, e);
            }
        }
        return false;
    }

    private static boolean openUrlForWindows(String url) {
        try {
            execPrivileged(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            return true;
        } catch (Exception e) {
            LOG.error("Failed to open URL for Windows: {}", url, e);
            return false;
        }
    }

    private static void execPrivileged(String[] cmd_array) throws PrivilegedActionException, IOException {
        Process process = java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Process>) () ->
                Runtime.getRuntime().exec(cmd_array)
        );
        // Close unused streams to make sure the child process won't hang
        process.getInputStream().close();
        process.getOutputStream().close();
        process.getErrorStream().close();
    }
}
