package lwjglalti.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DumbRingBufferTest {

    @Test
    void isFifo() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);

        bufferToTest.push(1);
        bufferToTest.push(2);

        assertEquals(1, bufferToTest.pop());
        assertEquals(2, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void popsAndPushesMayWrapAround() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.pop();
        bufferToTest.pop();
        // we should now be in the middle of the buffer, which means subsequent pushes should overflow

        bufferToTest.push(3);
        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.push(6);

        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void hasNextIsTrueWhenBufferIsNotFullButHasUnpoppedElement() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.push(3);

        bufferToTest.pop();
        bufferToTest.pop();

        assertTrue(bufferToTest.hasNext());
    }

    @Test
    void hasNextIsTrueWhenBufferIsFull() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.push(4);

        assertTrue(bufferToTest.hasNext());
    }

    @Test
    void hasNextIsTrueEvenWhenPushAndPopMarkersAreRelativelyWrapped() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.pop();
        bufferToTest.pop();
        bufferToTest.pop();
        // we should now be close to the end of the buffer, which means subsequent pushes should overflow

        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.push(6);

        assertTrue(bufferToTest.hasNext());
    }

    @Test
    void hasNextIsTrueEvenWhenPushAndPopMarkersAreRelativelyWrapped_andBufferIsFull() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.pop();
        bufferToTest.pop();
        bufferToTest.pop();
        // we should now be close to the end of the buffer, which means subsequent pushes should overflow

        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.push(6);
        bufferToTest.push(7);

        assertTrue(bufferToTest.hasNext());
    }

    @Test
    void popsAndPushesMayBeInterleaved() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        assertEquals(1, bufferToTest.pop());
        bufferToTest.push(2);
        assertEquals(2, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void popsAndPushesMayBeInterleaved_evenWithMoreElementsRemaining() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        assertEquals(1, bufferToTest.pop());
        bufferToTest.push(3);
        assertEquals(2, bufferToTest.pop());
        assertTrue(bufferToTest.hasNext());
    }

    @Test
    void popsAndPushesMayWrapAround_evenWithCapacityOne() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(1);

        bufferToTest.push(1);
        assertEquals(1, bufferToTest.pop());
        bufferToTest.push(2);
        assertEquals(2, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);

        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.resizeIfFull();
        bufferToTest.push(3);

        assertEquals(1, bufferToTest.pop());
        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes_evenWhenMultiple() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);

        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.resizeIfFull();
        bufferToTest.push(3);
        bufferToTest.push(4);

        assertEquals(1, bufferToTest.pop());
        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoublesSize() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);

        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.resizeIfFull();
        bufferToTest.push(3);
        bufferToTest.push(4);

        assertEquals(1, bufferToTest.pop());
        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoubleSize_evenWhenMarkIsNotAtZero() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);
        bufferToTest.push(1);
        bufferToTest.pop();

        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.resizeIfFull();
        bufferToTest.push(4);
        bufferToTest.push(5);

        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void bufferMayBeResizedMultipleTimes() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);

        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.resizeIfFull();
        bufferToTest.push(3);
        bufferToTest.push(4);
        bufferToTest.resizeIfFull();
        bufferToTest.push(5);
        bufferToTest.push(6);
        bufferToTest.push(7);

        assertEquals(1, bufferToTest.pop());
        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertEquals(7, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void bufferMayBeResizedMultipleTimes_evenWhenMarkIsNotAtZero() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(2);
        bufferToTest.push(1);
        bufferToTest.pop();

        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.resizeIfFull();
        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.resizeIfFull();
        bufferToTest.push(6);
        bufferToTest.push(7);
        bufferToTest.push(8);

        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertEquals(7, bufferToTest.pop());
        assertEquals(8, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes_evenWhenElementsWrapAround() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.pop();
        bufferToTest.pop();
        // we should now be in the middle of the buffer, which means subsequent pushes should overflow

        bufferToTest.push(3);
        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.push(6);
        bufferToTest.resizeIfFull();
        bufferToTest.push(7);

        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertEquals(7, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes_evenWhenMarkIsJustAfterFirst() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.pop();

        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.resizeIfFull();
        bufferToTest.push(6);

        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes_evenWhenMarkIsJustAfterLast() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.pop();
        bufferToTest.pop();
        bufferToTest.pop();

        bufferToTest.push(4);
        bufferToTest.push(5);
        bufferToTest.push(6);
        bufferToTest.push(7);
        bufferToTest.resizeIfFull();
        bufferToTest.push(8);

        assertEquals(4, bufferToTest.pop());
        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertEquals(7, bufferToTest.pop());
        assertEquals(8, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes_whenNotFull() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(3);

        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.resizeIfFull();
        bufferToTest.push(3);

        assertEquals(1, bufferToTest.pop());
        assertEquals(2, bufferToTest.pop());
        assertEquals(3, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }

    @Test
    void resizeDoesNotAffectPreviousPushes_evenWhenElementsHaveFullyWrappedAround() {
        DumbRingBuffer bufferToTest = new DumbRingBuffer(4);
        bufferToTest.push(1);
        bufferToTest.push(2);
        bufferToTest.push(3);
        bufferToTest.push(4);
        bufferToTest.pop();
        bufferToTest.pop();
        bufferToTest.pop();
        bufferToTest.pop();
        // number of pushes and pops each equal the capacity: we should be back at the start

        bufferToTest.push(5);
        bufferToTest.push(6);
        bufferToTest.push(7);
        bufferToTest.push(8);
        bufferToTest.resizeIfFull();
        bufferToTest.push(9);

        assertEquals(5, bufferToTest.pop());
        assertEquals(6, bufferToTest.pop());
        assertEquals(7, bufferToTest.pop());
        assertEquals(8, bufferToTest.pop());
        assertEquals(9, bufferToTest.pop());
        assertFalse(bufferToTest.hasNext());
    }
}
