package me.rerere.matrix.util;

import java.util.List;

/** Kotlin MutableList delegation wrapper, recovered from each direct List invocation. */
public final class DelegatingMutableList<E> implements List<E>, me.rerere.matrix.thirdparty.kotlin.jvm.internal.markers.KMutableList {
    private final List<E> delegate;
    public static final ObjectListFactory companion;

    static {
        companion = new ObjectListFactory(
                (me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker) null);
    }

    public DelegatingMutableList(List<E> delegate) { this.delegate = delegate; }
    public boolean contains(Object value) { return delegate.contains(value); }
    public boolean removeAll(java.util.Collection<?> values) { return delegate.removeAll(values); }
    public int lastIndexOf(Object value) { return delegate.lastIndexOf(value); }
    public List<E> subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }
    public List<E> backingList() { return delegate; }
    public boolean addAll(int index, java.util.Collection<? extends E> values) {
        return delegate.addAll(index, values);
    }
    public java.util.ListIterator<E> listIterator() { return delegate.listIterator(); }
    public E removeAt(int index) { return delegate.remove(index); }
    public void add(int index, E element) { delegate.add(index, element); }
    public boolean retainAll(java.util.Collection<?> values) { return delegate.retainAll(values); }
    public Object[] toArray(Object[] destination) {
        return me.rerere.matrix.thirdparty.kotlin.jvm.internal.CollectionToArray.toArray(
                this, destination);
    }
    public E get(int index) { return delegate.get(index); }
    public void clear() { delegate.clear(); }
    public java.util.Iterator<E> iterator() { return delegate.iterator(); }
    public boolean containsAll(java.util.Collection<?> values) { return delegate.containsAll(values); }
    public int size() { return delegate.size(); }
    public int indexOf(Object value) { return delegate.indexOf(value); }
    public boolean add(E e) { return delegate.add(e); }
    public boolean remove(Object value) { return delegate.remove(value); }
    public E remove(int index) {
        return removeAt(index);
    }
    public boolean addAll(java.util.Collection<? extends E> values) {
        return delegate.addAll(values);
    }
    public boolean isEmpty() { return delegate.isEmpty(); }
    public Object[] toArray() {
        return me.rerere.matrix.thirdparty.kotlin.jvm.internal.CollectionToArray.toArray(this);
    }
    public java.util.ListIterator<E> listIterator(int index) { return delegate.listIterator(index); }
    public E set(int index, E element) { return delegate.set(index, element); }
}
