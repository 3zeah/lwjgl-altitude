package org.lwjgl.input;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardTest {

    @AfterEach
    void afterEach() {
        // no event pollution
        assertFalse(Keyboard.next());
    }

    @Test
    void keyEventWithoutSucceedingCharEventBecomesAnEventIfPolled() {
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS, 0);

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals(Keyboard.CHAR_NONE, Keyboard.getEventCharacter());
    }

    @Test
    void keyEventHasNoCharacterIfSucceededByAnotherKeyEvent() {
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS, 0);
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_V, GLFW.GLFW_PRESS, 0);

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals(Keyboard.CHAR_NONE, Keyboard.getEventCharacter());
        assertTrue(Keyboard.next());
    }

    @Test
    void keyEventSucceededByCharEventBecomesAnEventIfPolled() {
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS, 0);
        Keyboard.registerGlfwCharEvent('d');

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals('d', Keyboard.getEventCharacter());
    }

    @Test
    void characterAppliesExactlyToImmediatelyPrecedingKeyEvent() {
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS, 0);
        Keyboard.registerGlfwCharEvent('d');
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_V, GLFW.GLFW_PRESS, 0);

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals('d', Keyboard.getEventCharacter());
        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_NONE, Keyboard.getEventCharacter());
    }

    @Test
    void onlyFirstCharacterIsEmitted_whenNoSucceedingEvent() {
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS, 0);
        Keyboard.registerGlfwCharEvent('d');
        Keyboard.registerGlfwCharEvent('v');

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals('d', Keyboard.getEventCharacter());
    }

    @Test
    void onlyFirstCharacterIsEmitted_whenSucceedingEvent() {
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_PRESS, 0);
        Keyboard.registerGlfwCharEvent('d');
        Keyboard.registerGlfwCharEvent('v');
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_V, GLFW.GLFW_PRESS, 0);

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals('d', Keyboard.getEventCharacter());
        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_NONE, Keyboard.getEventCharacter());
    }

    @Test
    void releasesAreNeverCharacterEvents() {
        Keyboard.registerGlfwCharEvent('d');
        Keyboard.registerGlfwKeyEvent(GLFW.GLFW_KEY_D, GLFW.GLFW_RELEASE, 0);
        Keyboard.registerGlfwCharEvent('d');

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_D, Keyboard.getEventKey());
        assertEquals(Keyboard.KEY_NONE, Keyboard.getEventCharacter());
    }
}
