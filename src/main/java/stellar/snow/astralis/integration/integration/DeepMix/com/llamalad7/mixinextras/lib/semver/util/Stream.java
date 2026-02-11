package com.llamalad7.mixinextras.lib.semver.util;

import com.llamalad7.mixinextras.lib.semver.util.UnexpectedElementException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Stream<E>
implements Iterable<E> {
    private final E[] elements;
    private int offset = 0;

    public Stream(E[] elements) {
        this.elements = (Object[])elements.clone();
    }

    public E consume() {
        if (this.offset >= this.elements.length) {
            return null;
        }
        return this.elements[this.offset++];
    }

    @SafeVarargs
    public final <T extends ElementType<E>> E consume(T ... expected) {
        E lookahead = this.lookahead(1);
        for (T type : expected) {
            if (!type.isMatchedBy(lookahead)) continue;
            return this.consume();
        }
        throw new UnexpectedElementException(lookahead, this.offset, (ElementType<?>[])expected);
    }

    public E lookahead(int position) {
        int idx = this.offset + position - 1;
        if (idx < this.elements.length) {
            return this.elements[idx];
        }
        return null;
    }

    public int currentOffset() {
        return this.offset;
    }

    @SafeVarargs
    public final <T extends ElementType<E>> boolean positiveLookahead(T ... expected) {
        for (T type : expected) {
            if (!type.isMatchedBy(this.lookahead(1))) continue;
            return true;
        }
        return false;
    }

    @SafeVarargs
    public final <T extends ElementType<E>> boolean positiveLookaheadBefore(ElementType<E> before, T ... expected) {
        E lookahead;
        for (int i = 1; i <= this.elements.length && !before.isMatchedBy(lookahead = this.lookahead(i)); ++i) {
            for (T type : expected) {
                if (!type.isMatchedBy(lookahead)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>(){
            private int index;
            {
                this.index = Stream.this.offset;
            }

            @Override
            public boolean hasNext() {
                return this.index < Stream.this.elements.length;
            }

            @Override
            public E next() {
                if (this.index >= Stream.this.elements.length) {
                    throw new NoSuchElementException();
                }
                return Stream.this.elements[this.index++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static interface ElementType<E> {
        public boolean isMatchedBy(E var1);
    }
}

