package edu.cmu.ml.rtw.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This code is based on an example by Erik Ras, 
 * http://erikras.com/2008/01/18/the-filter-pattern-java-conditional-abstraction-with-iterables/
 * with modifications to allow the output to have a different type from the input.
 */

public abstract class TypeChangingFilter<T,V> {
  public abstract boolean passes(T object);
  public abstract V convert(T object);
  public Iterator<V> filter(Iterator<T> iterator) {
    return new FilterIterator(iterator);
  }
  public Iterable<V> filter(final Iterable<T> iterable) {
    return new Iterable<V>() {
      public Iterator<V> iterator() {
        return filter(iterable.iterator());
      }
    };
  }
  private class FilterIterator implements Iterator<V> {
    private Iterator<T> iterator;
    private T next;
    private FilterIterator(Iterator<T> iterator) {
      this.iterator = iterator;
      toNext();
    }
    public boolean hasNext() {
      return next != null;
    }
    public V next() {
      if (next == null)
        throw new NoSuchElementException();
      T passedValue = next;
      toNext();
      return convert(passedValue);
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
    private void toNext() {
      next = null;
      while (iterator.hasNext()) {
        T item = iterator.next();
        if (item != null && passes(item)) {
          next = item;
          break;
        }
      }
    }
  }
}

