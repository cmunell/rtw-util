package edu.cmu.ml.rtw.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A version of LRUCache that manages greater speed by offering an API different from Cache
 *
 * The crux of the change is that this implementation avoids constructing new objects when pushing
 * old values out of the cache, especially both the Item metadata objects stored in the cache and
 * the object used to convey the key and value of the decached item back to the caller.  The prior
 * version, when used in tight loops, was capable of producing several times more than 100% CPU
 * usage that came apparently from stress on garbage collection or some other kind of object
 * management (in fact, it did not show up as being accounted to the garbage collection threads or
 * any other threads!).
 *
 * It might have been appropriate to have simply changed the Cache interface to accomodate this new
 * formulation.  If Cache and LRUCache become vestigial, then changing or eliminating them might be
 * a good idea.
 *
 * Note: this is not threadsafe (and probably should be left that way for speed).
 */
public class FasterLRUCache<K, V> implements Iterable<FasterLRUCache.Item<K, V>> {
    private final static Logger log = LogFactory.getLogger();

    /**
     * Internal class used as value type for our main hash map, but with some public methods so that
     * we can use it for the return value of our put method as well.
     *
     * This is independently templatized so that it can be a static class, making it faster.
     */
    public final static class Item<K, V> {
        private K key;
        private V value;
        private Item previous;
        private Item next;
        private boolean dirty;

        public Item() {
        }

        public Item(K k, V v) {
            key = k;
            value = v;
        }

        public Item(K k, V v, boolean d) {
            key = k;
            value = v;
            dirty = d;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void clear() {
            key = null;
            value = null;
            previous = null;
            next = null;
            dirty = false;
        }
    }

    /**
     * Iterator class
     */
    public final  class MyIterator implements Iterator<Item<K, V>> {
        /**
         * Underlying iterator -- traverses the linked list.
         */
        private Item<K, V> it;

        /**
         * Wheather or not to skip clean elements
         */
        private final boolean skipClean;

        /**
         * Whether or not to mark elements clean as we return them
         */
        private final boolean cleanDirty;

        /**
         * Constructor
         */
        private MyIterator(final boolean skipClean, final boolean cleanDirty) {
            this.skipClean = skipClean;
            this.cleanDirty = cleanDirty;
            it = m_start;
            if (skipClean)
                while (!it.dirty && it != m_end) it = it.next;
            if (it == m_end) it = null;
        }

        public final boolean hasNext() { 
            return it != null;
        } 
    
        public final Item<K, V> next() { 
            if (!hasNext()) throw new java.util.NoSuchElementException(); 

            Item<K, V> tmp = it;
            if (cleanDirty) tmp.dirty = false;
            it = it.next;
            if (skipClean)
                while (!it.dirty && it != m_end) it = it.next;
            if (it == m_end) it = null;

            return tmp;
        } 

        public void remove() { 
            throw new java.lang.UnsupportedOperationException(); 
        }
    }

    private final Map<K, Item<K, V>> m_map = new HashMap<K, Item<K, V>>();
    private Item<K, V> m_start;
    private Item<K, V> m_end;
    private final int m_maxSize;
    private int gets;
    private int puts;
    private int getHits;
    private int putHits;

    private void removeItem(Item<K, V> item) {
        item.previous.next = item.next;
        item.next.previous = item.previous;
    }

    private void insertHead(Item<K, V> item) {
        item.previous = m_start;
        item.next = m_start.next;
        m_start.next.previous = item;
        m_start.next = item;
    }

    private void moveToHead(Item<K, V> item) {
        item.previous.next = item.next;
        item.next.previous = item.previous;
        item.previous = m_start;
        item.next = m_start.next;
        m_start.next.previous = item;
        m_start.next = item;
    }

    /**
     * Constructor
     */
    public FasterLRUCache(int maxObjects) {
        m_maxSize = maxObjects;
        clear();
    }

    /**
     * Get number of elements in the cache
     */
    public int size() {
        return m_map.size();
    }

    /**
     * Clear the cache
     */
    public void clear() {
        m_map.clear();
        m_start = new Item<K, V>();
        m_end = new Item<K, V>();
        m_start.next = m_end;
        m_end.previous = m_start;

        gets = 0;
        puts = 0;
        getHits = 0;
        putHits = 0;
    }

    /**
     * Get key, returning null if not present in the cache
     */
    public V get(K key) {
        gets++;
        Item<K, V> cur = m_map.get(key);
        if (cur == null) {
            return null;
        }
        if (cur != m_start.next) {
            moveToHead(cur);
        }
        getHits++;
        return (V) cur.value;
    }

    /**
     * Put a new element in the cache and possibly recieve an old one if one had to be decached in
     * order to maintain the maximum cache size.
     *
     * In order to support use in implementations of java.util.Map, this returns the value
     * previously associated with the given key, or null if there was none.
     *
     * If an old and dirty element had to be kicked out of the cache, dst will be set to that
     * element.  dst will be left untouched if no element had to be kicked out or if the element
     * kicked out was not dirty.  The caller therefore should invoke dst's clear method before
     * calling this in order to be able to determine whether or not dst contains a dirty element
     * upon return.
     *
     * Use of the dst parameter is necessary because Java offers no facility for a function to
     * return more than one object, which in turn necessitates the use of some kind of temporary
     * tuple object, which in turn incurrs object construction and garbage collection costs, which
     * is an unnacceptable overhead for some uses of these caches.  Thus, the calling code maintains
     * a single instance of this class, and the put method is given a pointer to it in order that it
     * might repopulate it with a decached element.
     */
    public V put(K key, V value, boolean dirty, Item<K, V> dst) {
        puts++;
        Item<K, V> cur = m_map.get(key);
        if (cur != null) {
            putHits++;
            V prev = cur.value;
            cur.value = value;
            cur.dirty = cur.dirty || dirty;
            moveToHead(cur);
            return prev;
        }

        if (m_map.size() >= m_maxSize) {
            cur = m_end.previous;
            if (cur.dirty) {
                dst.key = cur.key;
                dst.value = cur.value;
            }
            m_map.remove(cur.key);
            cur.key = key;
            cur.value = value;
            cur.dirty = dirty;
            m_map.put(key, cur);
            moveToHead(cur);
        } else {
            cur = new Item<K, V>(key, value, dirty);
            insertHead(cur);
            m_map.put(key, cur);
        }
        return null;
    }

    /**
     * Convenient alternative to the regular put for when you never have dirty entries and don't
     * need to worry about decached items.
     */
    public V put(K key, V value) {
        puts++;
        Item<K, V> cur = m_map.get(key);
        if (cur != null) {
            putHits++;
            V prev = cur.value;
            cur.value = value;
            moveToHead(cur);
            return prev;
        }

        if (m_map.size() >= m_maxSize) {
            cur = m_end.previous;
            if (cur.dirty)
                throw new RuntimeException("Cache item was dirty.  You can't use this version of put!");
            m_map.remove(cur.key);
            cur.key = key;
            cur.value = value;
            cur.dirty = false;
            m_map.put(key, cur);
            moveToHead(cur);
        } else {
            cur = new Item<K, V>(key, value, false);
            insertHead(cur);
            m_map.put(key, cur);
        }
        return null;
    }

    /**
     * Remove an element from the cache
     *
     * Behavior does not depend on the dirtiness of the element.  This is a no-op if the element is
     * not present in the cache.
     */
    public void remove(K key) {
        Item<K, V> cur = m_map.get(key);
        if (cur == null) {
            return;
        }
        m_map.remove(key);
        removeItem(cur);
    }


    /**
     * Iterate over all elements in the cache
     */
    public Iterator<Item<K, V>> iterator() {
        return new MyIterator(false, false);
    }

    /**
     * Iterate over all the dirty elements in the cache
     */
    public Iterator<Item<K, V>> dirtyIterator() {
        return new MyIterator(true, false);
    }

    /**
     * Iterate over all the dirty elements in the cache, marking each clean as it is visited
     */
    public Iterator<Item<K, V>> cleaningDirtyIterator() {
        return new MyIterator(true, true);
    }

    /**
     * Return one-liner about cache statistics suitable for logging
     */
    public String logStats() {
        double getHitRate = 100.0 * (double)getHits / (double)gets;
        double putHitRate = 100.0 * (double)putHits / (double)puts;
        
        return "size " + size() + ", "
                + gets + " gets, " + getHits + " hits (" + String.format("%.02f", getHitRate) + "%), "
                + puts + " puts, " + putHits + " hits (" + String.format("%.02f", putHitRate) + "%)";
    }
}
