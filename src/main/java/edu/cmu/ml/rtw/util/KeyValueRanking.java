package edu.cmu.ml.rtw.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to make it easy to find the top k <key,value> pairs when sorting by
 * the keys. Maintains only a list of the top k items, saving heap space. Fully
 * generic with a convenient factory method.
 * 
 * Example Usage: KeyValueRanking<Double, String> kvr =
 * KeyValueRanking.newAscendingKeyValueRanking(3); kvr.insertItem(1.0, "a");
 * kvr.insertItem(-1.0, "b"); kvr.insertItem(3.0, "c"); kvr.insertItem(5.0,
 * "d"); kvr.insertItem(-4.0, "e"); kvr.insertItem(3.0, "f");
 * kvr.insertItem(-20.0, "g");
 * 
 * for (Double d : kvr.getKeys()) { System.out.println(d); } for (String s :
 * kvr.getValues()) { System.out.println(s); }
 * 
 * 
 * @author acarlson
 * 
 * @param <K>
 * @param <V>
 */
public class KeyValueRanking<K extends Comparable<? super K>, V> {
    protected final List<K> keys;
    protected final List<V> values;

    protected final int k;
    protected final Ordering ordering;

    public enum Ordering {
        ASCENDING, DESCENDING
    };

    public KeyValueRanking(int k, Ordering o) {
        this.k = k;
        this.ordering = o;
        this.keys = new ArrayList<K>(k + 1);
        this.values = new ArrayList<V>(k + 1);
    }

    public void clear() {
        keys.clear();
        values.clear();
    }

    public int size() {
        return keys.size();
    }

    public void insertItem(K key, V value) {
        int index;
        if (ordering == Ordering.ASCENDING)
            index = Collections.binarySearch(keys, key);
        else
            index = Collections.binarySearch(keys, key, Collections.reverseOrder());
        if (index < 0) {
            index = -index - 1;
        }
        if (index < k) {
            if (index == keys.size()) {
                keys.add(key);
                values.add(value);
            } else {
                keys.add(index, key);
                values.add(index, value);
            }
        }

        if (keys.size() > k) {
            keys.remove(k);
            values.remove(k);
        }
    }

    public List<K> getKeys() {
        return keys;
    }

    public K getKey(int index) {
        return getKeys().get(index);
    }

    public List<V> getValues() {
        return values;
    }

    public V getValue(int index) {
        return getValues().get(index);
    }

    public int getK() {
        return k;
    }

    public static <K extends Comparable<? super K>, V> KeyValueRanking<K, V> newDescendingKeyValueRanking(
            int k) {
        return new KeyValueRanking<K, V>(k, Ordering.DESCENDING);
    }

    public static <K extends Comparable<? super K>, V> KeyValueRanking<K, V> newAscendingKeyValueRanking(
            int k) {
        return new KeyValueRanking<K, V>(k, Ordering.ASCENDING);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("(");
            sb.append(keys.get(i));
            sb.append(",");
            sb.append(values.get(i));
            sb.append(")");
        }
        return sb.toString();
    }

    // Some simple test code.
    public static void main(String[] args) {
        KeyValueRanking<Double, String> kvr = KeyValueRanking
                .<Double, String> newAscendingKeyValueRanking(3);
        kvr.insertItem(1.0, "a");
        kvr.insertItem(-1.0, "b");
        kvr.insertItem(3.0, "c");
        kvr.insertItem(5.0, "d");
        kvr.insertItem(-4.0, "e");
        kvr.insertItem(3.0, "f");
        kvr.insertItem(-20.0, "g");

        for (Double d : kvr.getKeys()) {
            System.out.println(d);
        }
        for (String s : kvr.getValues()) {
            System.out.println(s);
        }
    }
}
