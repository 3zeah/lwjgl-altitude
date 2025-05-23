/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.openal;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Checks;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.IntFunction;

import static org.lwjgl.openal.AL10.AL_EXTENSIONS;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.AL_VERSION;
import static org.lwjgl.openal.EXTThreadLocalContext.alcGetThreadContext;
import static org.lwjgl.system.APIUtil.APIVersion;
import static org.lwjgl.system.APIUtil.apiFilterExtensions;
import static org.lwjgl.system.APIUtil.apiLog;
import static org.lwjgl.system.APIUtil.apiLogMissing;
import static org.lwjgl.system.APIUtil.apiParseVersion;
import static org.lwjgl.system.JNI.invokeI;
import static org.lwjgl.system.JNI.invokeP;
import static org.lwjgl.system.JNI.invokePP;
import static org.lwjgl.system.JNI.invokePZ;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCIISafe;
import static org.lwjgl.system.MemoryUtil.memAddress;

/**
 * This class must be used before any OpenAL function is called. It has the following responsibilities:
 * <ul>
 * <li>Creates instances of {@link ALCapabilities} classes. An {@code ALCapabilities} instance contains flags for functionality that is available in an OpenAL
 * context. Internally, it also contains function pointers that are only valid in that specific OpenAL context.</li>
 * <li>Maintains thread-local and global state for {@code ALCapabilities} instances, corresponding to OpenAL contexts that are current in those threads and the
 * entire process, respectively.</li>
 * </ul>
 *
 * <h3>ALCapabilities creation</h3>
 * <p>Instances of {@code ALCapabilities} can be created with the {@link #createCapabilities} method. An OpenAL context must be current in the current thread
 * or process before it is called. Calling this method is expensive, so {@code ALCapabilities} instances should be cached in user code.</p>
 *
 * <h3>Thread-local state</h3>
 * <p>Before a function for a given OpenAL context can be called, the corresponding {@code ALCapabilities} instance must be made current in the current
 * thread or process. The user is also responsible for clearing the current {@code ALCapabilities} instance when the context is destroyed or made current in
 * another thread.</p>
 *
 * <p>Note that OpenAL contexts are made current process-wide by default. Current thread-local contexts are only available if the
 * {@link EXTThreadLocalContext ALC_EXT_thread_local_context} extension is supported by the OpenAL implementation. <em>OpenAL Soft</em>, the implementation
 * that LWJGL ships with, supports this extension and performs better when it is used.</p>
 *
 * @see ALC
 */
public final class AL {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ALCapabilities processCaps;

    private static final ThreadLocal<ALCapabilities> capabilitiesTLS = new ThreadLocal<>();

    private static ICD icd = new ICDStatic();

    private AL() {}

    static void init() {
    }

    public static void destroy() {
        if (context != NULL) {
            boolean contextReleased = ALC10.alcMakeContextCurrent(NULL);
            if (!contextReleased) {
                LOG.error("Failed to release OpenAL context");
            }
            ALC10.alcDestroyContext(context);
            context = NULL;
        }
        if (device != null) {
            boolean deviceClosed = ALC10.alcCloseDevice(device);
            if (!deviceClosed) {
                LOG.error("Failed to close audio device");
            }
            device = null;
        }

        setCurrentProcess(null);
        // ALC#destroy will call this method: prevent infinite recursion with a flag
        if (!created) {
            return;
        }
        created = false;
        ALC.destroy();
    }

    /**
     * Sets the specified {@link ALCapabilities} for the current process-wide OpenAL context.
     *
     * <p>If the current thread had a context current (see {@link #setCurrentThread}), those {@code ALCapabilities} are cleared. Any OpenAL functions called in
     * the current thread, or any threads that have no context current, will use the specified {@code ALCapabilities}.</p>
     *
     * @param caps the {@link ALCapabilities} to make current, or null
     */
    public static void setCurrentProcess(ALCapabilities caps) {
        processCaps = caps;
        capabilitiesTLS.set(null); // See EXT_thread_local_context, second Q.
        icd.set(caps);
    }

    /**
     * Sets the specified {@link ALCapabilities} for the current OpenAL context in the current thread.
     *
     * <p>Any OpenAL functions called in the current thread will use the specified {@code ALCapabilities}.</p>
     *
     * @param caps the {@link ALCapabilities} to make current, or null
     */
    public static void setCurrentThread(ALCapabilities caps) {
        capabilitiesTLS.set(caps);
        icd.set(caps);
    }

    /**
     * Returns the {@link ALCapabilities} for the OpenAL context that is current in the current thread or process.
     *
     * @throws IllegalStateException if no OpenAL context is current in the current thread or process
     */
    public static ALCapabilities getCapabilities() {
        ALCapabilities caps = capabilitiesTLS.get();
        if (caps == null) {
            caps = processCaps;
        }

        return checkCapabilities(caps);
    }

    private static ALCapabilities checkCapabilities(ALCapabilities caps) {
        if (caps == null) {
            throw new IllegalStateException(
                    "No ALCapabilities instance set for the current thread or process. Possible solutions:\n" +
                            "\ta) Call AL.createCapabilities() after making a context current.\n" +
                            "\tb) Call AL.setCurrentProcess() or AL.setCurrentThread() if an ALCapabilities instance already exists."
            );
        }
        return caps;
    }

    /**
     * Creates a new {@link ALCapabilities} instance for the OpenAL context that is current in the current thread or process.
     *
     * <p>This method calls {@link #setCurrentProcess} (or {@link #setCurrentThread} if applicable) with the new instance before returning.</p>
     *
     * @param alcCaps the {@link ALCCapabilities} of the device associated with the current context
     *
     * @return the ALCapabilities instance
     */
    public static ALCapabilities createCapabilities(ALCCapabilities alcCaps) {
        return createCapabilities(alcCaps, null);
    }

    /**
     * Creates a new {@link ALCapabilities} instance for the OpenAL context that is current in the current thread or process.
     *
     * @param alcCaps       the {@link ALCCapabilities} of the device associated with the current context
     * @param bufferFactory a function that allocates a {@link PointerBuffer} given a size. The buffer must be filled with zeroes. If {@code null}, LWJGL will
     *                      allocate a GC-managed buffer internally.
     *
     * @return the ALCapabilities instance
     */
    public static ALCapabilities createCapabilities(ALCCapabilities alcCaps, IntFunction<PointerBuffer> bufferFactory) {
        // We'll use alGetProcAddress for both core and extension entry points.
        // To do that, we need to first grab the alGetProcAddress function from
        // the OpenAL native library.
        long alGetProcAddress = ALC.getFunctionProvider().getFunctionAddress(NULL, "alGetProcAddress");
        if (alGetProcAddress == NULL) {
            throw new RuntimeException("A core AL function is missing. Make sure that the OpenAL library has been loaded correctly.");
        }

        FunctionProvider functionProvider = functionName -> {
            long address = invokePP(memAddress(functionName), alGetProcAddress);
            if (address == NULL && Checks.DEBUG_FUNCTIONS) {
                apiLogMissing("AL", functionName);
            }
            return address;
        };

        long GetString          = functionProvider.getFunctionAddress("alGetString");
        long GetError           = functionProvider.getFunctionAddress("alGetError");
        long IsExtensionPresent = functionProvider.getFunctionAddress("alIsExtensionPresent");
        if (GetString == NULL || GetError == NULL || IsExtensionPresent == NULL) {
            throw new IllegalStateException("Core OpenAL functions could not be found. Make sure that the OpenAL library has been loaded correctly.");
        }

        String versionString = memASCIISafe(invokeP(AL_VERSION, GetString));
        if (versionString == null || invokeI(GetError) != AL_NO_ERROR) {
            throw new IllegalStateException("There is no OpenAL context current in the current thread or process.");
        }

        APIVersion apiVersion = apiParseVersion(versionString);

        int majorVersion = apiVersion.major;
        int minorVersion = apiVersion.minor;

        int[][] AL_VERSIONS = {
                {0, 1}  // OpenAL 1
        };

        Set<String> supportedExtensions = new HashSet<>(32);

        for (int major = 1; major <= AL_VERSIONS.length; major++) {
            int[] minors = AL_VERSIONS[major - 1];
            for (int minor : minors) {
                if (major < majorVersion || (major == majorVersion && minor <= minorVersion)) {
                    supportedExtensions.add("OpenAL" + major + minor);
                }
            }
        }

        // Parse EXTENSIONS string
        String extensionsString = memASCIISafe(invokeP(AL_EXTENSIONS, GetString));
        if (extensionsString != null) {
            MemoryStack stack = stackGet();

            StringTokenizer tokenizer = new StringTokenizer(extensionsString);
            while (tokenizer.hasMoreTokens()) {
                String extName = tokenizer.nextToken();
                try (MemoryStack frame = stack.push()) {
                    if (invokePZ(memAddress(frame.ASCII(extName, true)), IsExtensionPresent)) {
                        supportedExtensions.add(extName);
                    }
                }
            }
        }

        if (alcCaps.ALC_EXT_EFX) {
            supportedExtensions.add("ALC_EXT_EFX");
        }
        apiFilterExtensions(supportedExtensions, Configuration.OPENAL_EXTENSION_FILTER);

        ALCapabilities caps = new ALCapabilities(functionProvider, supportedExtensions, bufferFactory == null ? BufferUtils::createPointerBuffer : bufferFactory);

        if (alcCaps.ALC_EXT_thread_local_context && alcGetThreadContext() != NULL) {
            setCurrentThread(caps);
        } else {
            setCurrentProcess(caps);
        }

        return caps;
    }

    static ALCapabilities getICD() {
        return ALC.check(icd.get());
    }

    /** Function pointer provider. */
    private interface ICD {
        default void set(ALCapabilities caps) {}
        ALCapabilities get();
    }

    /**
     * Write-once {@link ICD}.
     *
     * <p>This is the default implementation that skips the thread/process lookup. When a new ALCapabilities is set, we compare it to the write-once
     * capabilities. If different function pointers are found, we fall back to the expensive lookup. This will never happen with the OpenAL-Soft
     * implementation.</p>
     */
    private static class ICDStatic implements ICD {

        private static ALCapabilities tempCaps;

        @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
        @Override
        public void set(ALCapabilities caps) {
            if (tempCaps == null) {
                tempCaps = caps;
            } else if (caps != null && caps != tempCaps && ThreadLocalUtil.areCapabilitiesDifferent(tempCaps.addresses, caps.addresses)) {
                apiLog("[WARNING] Incompatible context detected. Falling back to thread/process lookup for AL contexts.");
                icd = AL::getCapabilities; // fall back to thread/process lookup
            }
        }

        @Override
        public ALCapabilities get() {
            return WriteOnce.caps;
        }

        private static final class WriteOnce {
            // This will be initialized the first time get() above is called
            static final ALCapabilities caps;

            static {
                ALCapabilities tempCaps = ICDStatic.tempCaps;
                if (tempCaps == null) {
                    throw new IllegalStateException("No ALCapabilities instance has been set");
                }
                caps = tempCaps;
            }
        }

    }

    // COMPATIBILITY EXTENSION BELOW

    private static boolean created = false;
    private static ALCdevice device = null;
    private static long context = NULL;

    // lwjgl2-style: create a single system-wide device with a current context
    public static void create(String specifier, int contextFrequency, int contextRefresh, boolean contextSynchronized)
            throws LWJGLException {
        if (created) {
            throw new IllegalStateException("OpenAL context was already created");
        }
        IntBuffer contextAttributes = packageContextAttributes(contextFrequency, contextRefresh, contextSynchronized);
        try {
            createContext(specifier, contextAttributes);
        } catch (Exception e) {
            destroy();
            // ALC#destroy and AL#destroy has an awkward circular dependency: ensure ALC is also destroyed
            ALC.destroy();
            throw e;
        }
        created = true;
    }

    private static IntBuffer packageContextAttributes(int frequency, int refresh, boolean sync) {
        IntBuffer contextAttributes = BufferUtils.createIntBuffer(7);

        contextAttributes.put(ALC10.ALC_FREQUENCY);
        contextAttributes.put(frequency);

        contextAttributes.put(ALC10.ALC_REFRESH);
        contextAttributes.put(refresh);

        contextAttributes.put(ALC10.ALC_SYNC);
        contextAttributes.put(sync ? ALC10.ALC_TRUE : ALC10.ALC_FALSE);

        contextAttributes.put(0); // 0-terminated
        contextAttributes.flip();
        return contextAttributes;
    }

    private static void createContext(CharSequence specifier, IntBuffer attributes) throws LWJGLException {
        long device = ALC10.alcOpenDevice(specifier);
        if (device == NULL) {
            throw new LWJGLException("Could not open audio device");
        }
        AL.device = new ALCdevice(device);
        long context = ALC10.alcCreateContext(device, attributes);
        if (context == NULL) {
            ALC10.alcCloseDevice(device);
            throw new LWJGLException("Could not create OpenAL context");
        }
        AL.context = context;
        boolean contextSuccess = ALC10.alcMakeContextCurrent(context);
        if (!contextSuccess) {
            ALC10.alcDestroyContext(context);
            ALC10.alcCloseDevice(device);
            throw new LWJGLException("Could not make OpenAL context current");
        }
        AL.createCapabilities(ALC.createCapabilities(device));
    }

    public static ALCdevice getDevice() {
        return device;
    }

    public static boolean isCreated() {
        return created;
    }
}
