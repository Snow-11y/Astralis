package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;

final class Hashing {
    static int smear(int hashCode) {
        return 461845907 * Integer.rotateLeft(hashCode * -862048943, 15);
    }

    static int smearedHash(@Nullable Object o) {
        return Hashing.smear(o == null ? 0 : o.hashCode());
    }

    static int closedTableSize(int expectedEntries, double loadFactor) {
        int tableSize;
        if ((expectedEntries = Math.max(expectedEntries, 2)) > (int)(loadFactor * (double)(tableSize = Integer.highestOneBit(expectedEntries)))) {
            return (tableSize <<= 1) > 0 ? tableSize : 0x40000000;
        }
        return tableSize;
    }

    static boolean needsResizing(int size, int tableSize, double loadFactor) {
        return (double)size > loadFactor * (double)tableSize && tableSize < 0x40000000;
    }
}

