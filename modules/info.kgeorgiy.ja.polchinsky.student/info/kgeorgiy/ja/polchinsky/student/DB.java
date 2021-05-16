package info.kgeorgiy.ja.polchinsky.student;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DB {

    protected final <E, K, V> Stream<Map.Entry<K, V>> groupBy(Collection<E> values,
                                                              Function<E, K> grouper,
                                                              Collector<E, ?, V> downstream) {
        return values.stream()
                .collect(Collectors.groupingBy(grouper, downstream))
                .entrySet().stream();
    }

    protected final <K, V extends Comparable<? super V>> K getKeyWithMaxValue(Stream<Map.Entry<K, V>> entryStream,
                                                                              Comparator<K> comparator,
                                                                              K defaultKey) {
        return entryStream
                .max(Map.Entry.<K, V>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(comparator)))
                .map(Map.Entry::getKey)
                .orElse(defaultKey);
    }

    protected final <T> Collector<T, ?, Integer> countingDistinctBy(Function<T, ?> distinctBy) {
        return Collectors.mapping(distinctBy, Collectors.collectingAndThen(Collectors.toSet(), Set::size));
    }

    protected final <E, T> Stream<T> map(List<E> values, Function<E, T> mapper) {
        return values.stream().map(mapper);
    }

    protected final <E, T> List<T> mapToList(List<E> values, Function<E, T> mapper) {
        return map(values, mapper)
                .collect(Collectors.toList());
    }

    protected final <E, T> List<T> mapToListByIndices(Collection<E> values, int[] indices, Function<E, T> mapper) {
        return Arrays.stream(indices)
                .mapToObj(List.copyOf(values)::get)
                .map(mapper)
                .collect(Collectors.toList());
    }

    protected final <E, T> SortedSet<T> mapToSortedSet(List<E> values, Function<E, T> mapper) {
        return map(values, mapper)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    protected final <T> List<T> toSortedList(Stream<T> stream, Comparator<T> comparator) {
        return stream
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    protected final <E> List<E> toSortedList(Collection<E> values, Comparator<E> comparator) {
        return toSortedList(values.stream(), comparator);
    }

    protected final <E, K> Stream<E> filterByKey(Collection<E> values, K key, Function<E, K> keyExtractor) {
        return values.stream()
                .filter(val -> key.equals(keyExtractor.apply(val)));
    }
}
