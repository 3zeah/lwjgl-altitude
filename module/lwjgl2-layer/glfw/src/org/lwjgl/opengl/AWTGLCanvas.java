package org.lwjgl.opengl;

import java.awt.*;

// this is the entry point for AWT rendering, which is only used for the level editor or manual rendering tests
// for now, it is unsupported for the following reasons
// * the primary purpose of this project is to improve the display management of the game itself: AWT is a separate
//    display manager
// * altitude maps are no longer actively created
// * AWT rendering is legacy
@SuppressWarnings("unused")
public class AWTGLCanvas extends Canvas {

    public AWTGLCanvas(PixelFormat __) {
    }

    protected void makeCurrent() {
        throw new UnsupportedOperationException("AWT rendering (in particular, the level editor) not supported");
    }

    protected void swapBuffers() {
        throw new UnsupportedOperationException("AWT rendering (in particular, the level editor) not supported");
    }
}
