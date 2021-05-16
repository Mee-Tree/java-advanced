package info.kgeorgiy.ja.polchinsky.arrayset;

import java.util.*;

@SuppressWarnings("unused")
public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final ReversibleList<E> array;
    private final Comparator<? super E> comparator;

    private static <E> ArraySet<E> withComparator(Comparator<? super E> comparator) {
        return new ArraySet<>(comparator);
    }

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(List.of(), comparator);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        Set<E> tmp = new TreeSet<>(comparator);
        tmp.addAll(collection);
        this.array = new ReversibleList<>(tmp);
        this.comparator = comparator;
    }

    private ArraySet(ReversibleList<E> sortedArray, Comparator<? super E> comparator) {
        this.array = sortedArray;
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return array.iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(array.reversed(), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object obj) {
        return indexOf((E) obj) >= 0;
    }

    private int indexOf(E element) {
        return Collections.binarySearch(array, Objects.requireNonNull(element), comparator);
    }

    private int lowerIndexOf(E element, boolean inclusive) {
        int index = indexOf(element);
        return index < 0 ? -(index + 1) - 1 :
                inclusive ? index : index - 1;
    }

    private int upperIndexOf(E element, boolean inclusive) {
        int index = indexOf(element);
        return index < 0 ? -(index + 1) :
                inclusive ? index : index + 1;
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }

        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }

    private NavigableSet<E> subSetImpl(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int fromIndex = upperIndexOf(fromElement, fromInclusive);
        int toIndex = lowerIndexOf(toElement, toInclusive);

        return fromIndex > toIndex ? withComparator(comparator) :
                new ArraySet<>(array.subList(fromIndex, toIndex + 1), comparator);
    }

    private int compare(E e1, E e2) {
        Comparator<? super E> cmp = comparator == null ?
                Collections.reverseOrder().reversed() : comparator;
        return cmp.compare(e1, e2);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return isEmpty() ? this : subSetImpl(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return isEmpty() ? this : subSetImpl(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        return get(0);
    }

    @Override
    public E last() {
        return get(size() - 1);
    }

    private E get(int index) {
        E element = getNullable(index);
        if (element == null) {
            throw new NoSuchElementException();
        }
        return element;
    }

    private E getNullable(int index) {
        return 0 <= index && index < size() ?
                array.get(index) : null;
    }

    @Override
    public E lower(E e) {
        return getNullable(lowerIndexOf(e, false));
    }

    @Override
    public E floor(E e) {
        return getNullable(lowerIndexOf(e, true));
    }

    @Override
    public E higher(E e) {
        return getNullable(upperIndexOf(e, false));
    }

    @Override
    public E ceiling(E e) {
        return getNullable(upperIndexOf(e, true));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private static class ReversibleList<E> extends AbstractList<E> implements RandomAccess {
        private final List<E> list;
        private final boolean reversed;

        ReversibleList(Collection<E> collection) {
            this(List.copyOf(collection), false);
        }

        private ReversibleList(List<E> list, boolean reversed) {
            this.list = list;
            this.reversed = reversed;
        }

        public ReversibleList<E> reversed() {
            return new ReversibleList<>(list, !reversed);
        }

        @Override
        public ReversibleList<E> subList(int fromIndex, int toIndex) {
            if (!reversed) {
                return new ReversibleList<>(list.subList(fromIndex, toIndex), false);
            }
            return new ReversibleList<>(list.subList(
                    reversedIndex(toIndex - 1), reversedIndex(fromIndex - 1)), true);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public E get(int i) {
            return reversed ? list.get(reversedIndex(i)) : list.get(i);
        }

        private int reversedIndex(int index) {
            return size() - index - 1;
        }
    }
}
