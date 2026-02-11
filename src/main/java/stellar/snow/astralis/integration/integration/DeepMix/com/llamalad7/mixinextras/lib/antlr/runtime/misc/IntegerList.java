package com.llamalad7.mixinextras.lib.antlr.runtime.misc;

import java.util.Arrays;

public class IntegerList {
    private static final int[] EMPTY_DATA = new int[0];
    private int[] _data = EMPTY_DATA;
    private int _size;

    public final void add(int value) {
        if (this._data.length == this._size) {
            this.ensureCapacity(this._size + 1);
        }
        this._data[this._size] = value;
        ++this._size;
    }

    public final int get(int index) {
        if (index < 0 || index >= this._size) {
            throw new IndexOutOfBoundsException();
        }
        return this._data[index];
    }

    public final int removeAt(int index) {
        int value = this.get(index);
        System.arraycopy(this._data, index + 1, this._data, index, this._size - index - 1);
        this._data[this._size - 1] = 0;
        --this._size;
        return value;
    }

    public final boolean isEmpty() {
        return this._size == 0;
    }

    public final int size() {
        return this._size;
    }

    public final void clear() {
        Arrays.fill(this._data, 0, this._size, 0);
        this._size = 0;
    }

    public final int[] toArray() {
        if (this._size == 0) {
            return EMPTY_DATA;
        }
        return Arrays.copyOf(this._data, this._size);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IntegerList)) {
            return false;
        }
        IntegerList other = (IntegerList)o;
        if (this._size != other._size) {
            return false;
        }
        for (int i = 0; i < this._size; ++i) {
            if (this._data[i] == other._data[i]) continue;
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < this._size; ++i) {
            hashCode = 31 * hashCode + this._data[i];
        }
        return hashCode;
    }

    public String toString() {
        return Arrays.toString(this.toArray());
    }

    private void ensureCapacity(int capacity) {
        if (capacity < 0 || capacity > 0x7FFFFFF7) {
            throw new OutOfMemoryError();
        }
        int newLength = this._data.length == 0 ? 4 : this._data.length;
        while (newLength < capacity) {
            if ((newLength *= 2) >= 0 && newLength <= 0x7FFFFFF7) continue;
            newLength = 0x7FFFFFF7;
        }
        this._data = Arrays.copyOf(this._data, newLength);
    }
}

