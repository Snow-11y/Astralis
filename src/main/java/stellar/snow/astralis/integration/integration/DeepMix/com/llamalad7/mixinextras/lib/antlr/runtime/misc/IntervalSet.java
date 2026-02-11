package com.llamalad7.mixinextras.lib.antlr.runtime.misc;

import com.llamalad7.mixinextras.lib.antlr.runtime.Vocabulary;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IntervalSet
implements IntSet {
    public static final IntervalSet COMPLETE_CHAR_SET = IntervalSet.of(0, 0x10FFFF);
    public static final IntervalSet EMPTY_SET;
    protected List<Interval> intervals;
    protected boolean readonly;

    public IntervalSet(IntervalSet set) {
        this(new int[0]);
        this.addAll(set);
    }

    public IntervalSet(int ... els) {
        if (els == null) {
            this.intervals = new ArrayList<Interval>(2);
        } else {
            this.intervals = new ArrayList<Interval>(els.length);
            for (int e : els) {
                this.add(e);
            }
        }
    }

    public static IntervalSet of(int a) {
        IntervalSet s = new IntervalSet(new int[0]);
        s.add(a);
        return s;
    }

    public static IntervalSet of(int a, int b) {
        IntervalSet s = new IntervalSet(new int[0]);
        s.add(a, b);
        return s;
    }

    public void add(int el) {
        if (this.readonly) {
            throw new IllegalStateException("can't alter readonly IntervalSet");
        }
        this.add(el, el);
    }

    public void add(int a, int b) {
        this.add(Interval.of(a, b));
    }

    protected void add(Interval addition) {
        if (this.readonly) {
            throw new IllegalStateException("can't alter readonly IntervalSet");
        }
        if (addition.b < addition.a) {
            return;
        }
        ListIterator<Interval> iter = this.intervals.listIterator();
        while (iter.hasNext()) {
            Interval r = iter.next();
            if (addition.equals(r)) {
                return;
            }
            if (addition.adjacent(r) || !addition.disjoint(r)) {
                Interval next;
                Interval bigger = addition.union(r);
                iter.set(bigger);
                while (iter.hasNext() && (bigger.adjacent(next = iter.next()) || !bigger.disjoint(next))) {
                    iter.remove();
                    iter.previous();
                    iter.set(bigger.union(next));
                    iter.next();
                }
                return;
            }
            if (!addition.startsBeforeDisjoint(r)) continue;
            iter.previous();
            iter.add(addition);
            return;
        }
        this.intervals.add(addition);
    }

    public IntervalSet addAll(IntSet set) {
        if (set == null) {
            return this;
        }
        if (set instanceof IntervalSet) {
            IntervalSet other = (IntervalSet)set;
            int n = other.intervals.size();
            for (int i = 0; i < n; ++i) {
                Interval I = other.intervals.get(i);
                this.add(I.a, I.b);
            }
        } else {
            for (int value : set.toList()) {
                this.add(value);
            }
        }
        return this;
    }

    public IntervalSet complement(IntSet vocabulary) {
        IntervalSet vocabularyIS;
        if (vocabulary == null || vocabulary.isNil()) {
            return null;
        }
        if (vocabulary instanceof IntervalSet) {
            vocabularyIS = (IntervalSet)vocabulary;
        } else {
            vocabularyIS = new IntervalSet(new int[0]);
            vocabularyIS.addAll(vocabulary);
        }
        return vocabularyIS.subtract(this);
    }

    public IntervalSet subtract(IntSet a) {
        if (a == null || a.isNil()) {
            return new IntervalSet(this);
        }
        if (a instanceof IntervalSet) {
            return IntervalSet.subtract(this, (IntervalSet)a);
        }
        IntervalSet other = new IntervalSet(new int[0]);
        other.addAll(a);
        return IntervalSet.subtract(this, other);
    }

    public static IntervalSet subtract(IntervalSet left, IntervalSet right) {
        if (left == null || left.isNil()) {
            return new IntervalSet(new int[0]);
        }
        IntervalSet result = new IntervalSet(left);
        if (right == null || right.isNil()) {
            return result;
        }
        int resultI = 0;
        int rightI = 0;
        while (resultI < result.intervals.size() && rightI < right.intervals.size()) {
            Interval resultInterval = result.intervals.get(resultI);
            Interval rightInterval = right.intervals.get(rightI);
            if (rightInterval.b < resultInterval.a) {
                ++rightI;
                continue;
            }
            if (rightInterval.a > resultInterval.b) {
                ++resultI;
                continue;
            }
            Interval beforeCurrent = null;
            Interval afterCurrent = null;
            if (rightInterval.a > resultInterval.a) {
                beforeCurrent = new Interval(resultInterval.a, rightInterval.a - 1);
            }
            if (rightInterval.b < resultInterval.b) {
                afterCurrent = new Interval(rightInterval.b + 1, resultInterval.b);
            }
            if (beforeCurrent != null) {
                if (afterCurrent != null) {
                    result.intervals.set(resultI, beforeCurrent);
                    result.intervals.add(resultI + 1, afterCurrent);
                    ++resultI;
                    ++rightI;
                    continue;
                }
                result.intervals.set(resultI, beforeCurrent);
                ++resultI;
                continue;
            }
            if (afterCurrent != null) {
                result.intervals.set(resultI, afterCurrent);
                ++rightI;
                continue;
            }
            result.intervals.remove(resultI);
        }
        return result;
    }

    public IntervalSet or(IntSet a) {
        IntervalSet o = new IntervalSet(new int[0]);
        o.addAll(this);
        o.addAll(a);
        return o;
    }

    public boolean contains(int el) {
        int n = this.intervals.size();
        int l = 0;
        int r = n - 1;
        while (l <= r) {
            int m = (l + r) / 2;
            Interval I = this.intervals.get(m);
            int a = I.a;
            int b = I.b;
            if (b < el) {
                l = m + 1;
                continue;
            }
            if (a > el) {
                r = m - 1;
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isNil() {
        return this.intervals == null || this.intervals.isEmpty();
    }

    public int getMinElement() {
        if (this.isNil()) {
            throw new RuntimeException("set is empty");
        }
        return this.intervals.get((int)0).a;
    }

    public int hashCode() {
        int hash = MurmurHash.initialize();
        for (Interval I : this.intervals) {
            hash = MurmurHash.update(hash, I.a);
            hash = MurmurHash.update(hash, I.b);
        }
        hash = MurmurHash.finish(hash, this.intervals.size() * 2);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IntervalSet)) {
            return false;
        }
        IntervalSet other = (IntervalSet)obj;
        return this.intervals.equals(other.intervals);
    }

    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean elemAreChar) {
        StringBuilder buf = new StringBuilder();
        if (this.intervals == null || this.intervals.isEmpty()) {
            return "{}";
        }
        if (this.size() > 1) {
            buf.append("{");
        }
        Iterator<Interval> iter = this.intervals.iterator();
        while (iter.hasNext()) {
            Interval I = iter.next();
            int a = I.a;
            int b = I.b;
            if (a == b) {
                if (a == -1) {
                    buf.append("<EOF>");
                } else if (elemAreChar) {
                    buf.append("'").appendCodePoint(a).append("'");
                } else {
                    buf.append(a);
                }
            } else if (elemAreChar) {
                buf.append("'").appendCodePoint(a).append("'..'").appendCodePoint(b).append("'");
            } else {
                buf.append(a).append("..").append(b);
            }
            if (!iter.hasNext()) continue;
            buf.append(", ");
        }
        if (this.size() > 1) {
            buf.append("}");
        }
        return buf.toString();
    }

    public String toString(Vocabulary vocabulary) {
        StringBuilder buf = new StringBuilder();
        if (this.intervals == null || this.intervals.isEmpty()) {
            return "{}";
        }
        if (this.size() > 1) {
            buf.append("{");
        }
        Iterator<Interval> iter = this.intervals.iterator();
        while (iter.hasNext()) {
            Interval I = iter.next();
            int a = I.a;
            int b = I.b;
            if (a == b) {
                buf.append(this.elementName(vocabulary, a));
            } else {
                for (int i = a; i <= b; ++i) {
                    if (i > a) {
                        buf.append(", ");
                    }
                    buf.append(this.elementName(vocabulary, i));
                }
            }
            if (!iter.hasNext()) continue;
            buf.append(", ");
        }
        if (this.size() > 1) {
            buf.append("}");
        }
        return buf.toString();
    }

    protected String elementName(Vocabulary vocabulary, int a) {
        if (a == -1) {
            return "<EOF>";
        }
        if (a == -2) {
            return "<EPSILON>";
        }
        return vocabulary.getDisplayName(a);
    }

    public int size() {
        int n = 0;
        int numIntervals = this.intervals.size();
        if (numIntervals == 1) {
            Interval firstInterval = this.intervals.get(0);
            return firstInterval.b - firstInterval.a + 1;
        }
        for (int i = 0; i < numIntervals; ++i) {
            Interval I = this.intervals.get(i);
            n += I.b - I.a + 1;
        }
        return n;
    }

    @Override
    public List<Integer> toList() {
        ArrayList<Integer> values = new ArrayList<Integer>();
        int n = this.intervals.size();
        for (int i = 0; i < n; ++i) {
            Interval I = this.intervals.get(i);
            int a = I.a;
            int b = I.b;
            for (int v = a; v <= b; ++v) {
                values.add(v);
            }
        }
        return values;
    }

    public void remove(int el) {
        if (this.readonly) {
            throw new IllegalStateException("can't alter readonly IntervalSet");
        }
        int n = this.intervals.size();
        for (int i = 0; i < n; ++i) {
            Interval I = this.intervals.get(i);
            int a = I.a;
            int b = I.b;
            if (el < a) break;
            if (el == a && el == b) {
                this.intervals.remove(i);
                break;
            }
            if (el == a) {
                ++I.a;
                break;
            }
            if (el == b) {
                --I.b;
                break;
            }
            if (el <= a || el >= b) continue;
            int oldb = I.b;
            I.b = el - 1;
            this.add(el + 1, oldb);
        }
    }

    public void setReadonly(boolean readonly) {
        if (this.readonly && !readonly) {
            throw new IllegalStateException("can't alter readonly IntervalSet");
        }
        this.readonly = readonly;
    }

    static {
        COMPLETE_CHAR_SET.setReadonly(true);
        EMPTY_SET = new IntervalSet(new int[0]);
        EMPTY_SET.setReadonly(true);
    }
}

