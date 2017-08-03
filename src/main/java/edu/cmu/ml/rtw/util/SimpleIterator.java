package edu.cmu.ml.rtw.util;

import java.util.Iterator;

/**
 * Abstract base class providing boilerplate and a convenient buffering system for making it easier
 * to write Iterator classes
 *
 * We have many classes that need to iterate Iterator.  Many of those classes face two common
 * issues.  First, they tend to be read-only, and it's cluttersome to have to add the code to throw
 * an exception for the remove method.
 *
 * More importantly, most implementations don't have an easy way to know whether or not there is a
 * next value to return until they try to come up with it.  The typical thing to do use a buffer of
 * length 1, and have the next and haveNext methods operate on that buffer, and then to refill the
 * buffer as part of the implementation of next.  It's tiresome to keep doing that.
 *
 * So what we do here is require only that a getNext method be implemented under the condition that
 * a return value of null is taken to signal the end of iteration.  Then this class handles all of
 * the above as boilerplate.
 */
public abstract class SimpleIterator<T> implements Iterator<T> {
    // Our buffer.  Iteration is over when this is null.
    protected T nextValue = null;

    @Override public boolean hasNext() {
        return nextValue != null;
    }

    @Override public T next() {
        if (!hasNext()) throw new java.util.NoSuchElementException();
        T tmp = nextValue;
        nextValue = getNext();
        return tmp;
    }

    @Override public void remove() {
        throw new java.lang.UnsupportedOperationException();
    }

    /**
     * Implementations should override this.
     *
     * This should return the next element, or null if there wind up being no more elements to return.
     */
    protected abstract T getNext();

    /**
     * Implementations should invoke this at the end of their constructors in order to prime
     * SimpleIterator
     */
    protected void primeSimpleIterator() {
        nextValue = getNext();
    }
}
