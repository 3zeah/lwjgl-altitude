package org.lwjgl.opengl;

public class ContextCapabilities {

    public final boolean GL_EXT_texture_compression_s3tc;

    private ContextCapabilities(boolean GL_EXT_texture_compression_s3tc) {
        this.GL_EXT_texture_compression_s3tc = GL_EXT_texture_compression_s3tc;
    }

    public static ContextCapabilities adapt(GLCapabilities capabilities) {
        return new ContextCapabilities(capabilities.GL_EXT_texture_compression_s3tc);
    }
}
