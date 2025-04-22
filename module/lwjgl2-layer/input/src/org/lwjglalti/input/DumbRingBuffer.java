package org.lwjglalti.input;

/**
 * Ring buffer incapable of resizing itself if not prompted, and that cannot handle being full
 */
public class DumbRingBuffer {

    private int[] elements;
    private int readIndex = 0;
    private int writeIndex = 0;

    /**
     * Undefined behavior for non-positive capacity
     */
    public DumbRingBuffer(int initialCapacity) {
        // keep an extra sentry index to simplify distinguishing between full and empty buffers
        this(new int[initialCapacity + 1]);
    }

    private DumbRingBuffer(int[] elements) {
        this.elements = elements;
    }

    public boolean hasNext() {
        return readIndex != writeIndex;
    }

    /**
     * Undefined behavior if not {@link #hasNext}
     */
    public int pop() {
        int result = elements[readIndex];
        readIndex = incrementedIndex(readIndex);
        return result;
    }

    /**
     * Undefined behavior if {@link #isFull}
     */
    public void push(int value) {
        elements[writeIndex] = value;
        writeIndex = incrementedIndex(writeIndex);
    }

    public void resizeIfFull() {
        if (!isFull()) {
            return;
        }
        int[] newElements = new int[(elements.length - 1) * 2 + 1];
        int i = 0;
        while (hasNext()) {
            newElements[i++] = pop();
        }
        readIndex = 0;
        writeIndex = i;
        elements = newElements;
    }

    private boolean isFull() {
        return readIndex == incrementedIndex(writeIndex);
    }

    private int incrementedIndex(int i) {
        int result = i + 1;
        if (result == elements.length) {
            return 0;
        } else {
            return result;
        }
    }
}
