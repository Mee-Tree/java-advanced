package info.kgeorgiy.ja.polchinsky.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private final ParallelMapper mapper;

    public IterativeParallelism() {
        this(null);
    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return reduce(threads, values,
                stream -> stream.map(Objects::toString),
                stream -> flatten(stream).collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(final int threads,
                              final List<? extends T> values,
                              final Predicate<? super T> predicate) throws InterruptedException {
        return reduce(threads, values,
                stream -> stream.filter(predicate),
                stream -> flatten(stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(final int threads,
                              final List<? extends T> values,
                              final Function<? super T, ? extends U> f) throws InterruptedException {
        return reduce(threads, values,
                stream -> stream.map(f),
                stream -> flatten(stream).collect(Collectors.toList()));
    }

    private <T> Stream<? extends T> flatten(final Stream<? extends Stream<? extends T>> stream) {
        return stream.flatMap(Function.identity());
    }

    @Override
    public <T> T maximum(final int threads,
                         final List<? extends T> values,
                         final Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> T minimum(final int threads,
                         final List<? extends T> values,
                         final Comparator<? super T> comparator) throws InterruptedException {
        return reduce(threads, values,
                stream -> stream.min(comparator).orElseThrow(),
                stream -> stream.min(comparator).orElseThrow());
    }

    @Override
    public <T> boolean all(final int threads,
                           final List<? extends T> values,
                           final Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(final int threads,
                           final List<? extends T> values,
                           final Predicate<? super T> predicate) throws InterruptedException {
        return reduce(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    private <T, A, R> R reduce(final int threadNum,
                               final List<? extends T> values,
                               final Function<Stream<? extends T>, A> function,
                               final Function<Stream<A>, R> finisher) throws InterruptedException {
        final List<Stream<? extends T>> chunks = split(threadNum, values);
        final List<A> result;

        if (mapper != null) {
            result = mapper.map(function, chunks);
        } else {
            result = defaultMap(function, chunks);
        }

        return finisher.apply(result.stream());
    }

    private <T, A> List<A> defaultMap(final Function<Stream<? extends T>, A> function,
                                      final List<Stream<? extends T>> chunks) throws InterruptedException {
        final List<A> result = new ArrayList<>(Collections.nCopies(chunks.size(), null));
        final List<Thread> threads = IntStream.range(0, chunks.size())
                .mapToObj(i -> {
                    final Thread thread = new Thread(() ->
                            result.set(i, function.apply(chunks.get(i))));
                    thread.start();
                    return thread;
                })
                .collect(Collectors.toList());

        joinAll(threads);
        return result;
    }

    private void joinAll(final List<Thread> threads) throws InterruptedException {
        for (int i = 0; i < threads.size(); ++i) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException ie) {
                for (int j = i; j < threads.size(); ++j) {
                    threads.get(j).interrupt();
                }

                for (int j = i; j < threads.size(); ) {
                    try {
                        threads.get(i).join();
                        ++j;
                    } catch (final InterruptedException e) {
                        ie.addSuppressed(e);
                    }
                }
                throw ie;
            }
        }
    }

    private <T> List<Stream<? extends T>> split(final int threads, final List<? extends T> values) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads cannot be less than one");
        }

        final List<Stream<? extends T>> chunks = new ArrayList<>();
        final int groupSize = values.size() / threads;
        final int reminder = values.size() % threads;
        int index = 0;

        for (int i = 0; i < threads && index < values.size(); ++i) {
            final int size = groupSize + (i < reminder ? 1 : 0);
            if (size > 0) {
                chunks.add(values.subList(index, index + size).stream());
                index += size;
            }
        }

        return chunks;
    }
}
