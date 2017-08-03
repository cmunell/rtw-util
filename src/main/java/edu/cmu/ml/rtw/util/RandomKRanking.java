package edu.cmu.ml.rtw.util;

import java.util.Random;

/**
 * Subclass of KeyValueRanking that instead takes a random K <key, value> pairs where K is above
 * some threshold
 */
public class RandomKRanking<K extends Comparable<? super K>, V> extends KeyValueRanking<K, V> {
    protected final K minimumThreshold;
    protected final Random random;
    protected int numConsidered;

    public RandomKRanking(int k, K minimumThreshold) {
        super(k, KeyValueRanking.Ordering.ASCENDING);
        this.minimumThreshold = minimumThreshold;
        this.random = new Random();
        this.numConsidered = 0;
    }

    @Override public void insertItem(K key, V value) {
        // Method taken from the paper "A Convenient Algorithm for Drawing a Simple Random
        // Sample" by McLeod (ripped off from SEALExpander.java)

        if (key.compareTo(minimumThreshold) <= 0) return;
        numConsidered++;
        if (keys.size() < k) {
            keys.add(key);
            values.add(value);
        } else if (random.nextFloat() < ((float)k / (float)numConsidered)) {
            int loser = random.nextInt(keys.size());
            keys.set(loser, key);
            values.set(loser, value);
        }
    }
}

