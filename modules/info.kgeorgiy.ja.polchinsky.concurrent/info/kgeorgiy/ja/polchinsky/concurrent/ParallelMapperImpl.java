package info.kgeorgiy.ja.polchinsky.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final BlockingRunnableQueue queue;
    private final List<Thread> threads;

    private volatile boolean closed = false;

    public ParallelMapperImpl(final int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Number of thread should be at least one");
        }
        this.queue = new BlockingRunnableQueue();

        final Runnable runnable = () -> {
            try {
                while (!Thread.interrupted()) {
                    queue.poll().run();
                }
            } catch (final InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };

        this.threads = Stream.generate(() -> new Thread(runnable))
                .limit(threads)
                .collect(Collectors.toList());
        this.threads.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args)
            throws InterruptedException {
        if (args.isEmpty()) {
            return Collections.emptyList();
        }

        final CountDown latch = new CountDown(args.size());
        final List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));

        IntStream.range(0, args.size()).forEach(i ->
                queue.add(() -> {
                    result.set(i, f.apply(args.get(i)));
                    latch.countDown();
                }));
        latch.await();
        return result;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        threads.forEach(Thread::interrupt);
        threads.forEach(thread -> {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException ignored) {
                }
            }
        });
    }

    private static class BlockingRunnableQueue {
        private final Queue<Runnable> queue;

        public BlockingRunnableQueue() {
            this.queue = new ArrayDeque<>();
        }

        public synchronized Runnable poll() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }

        public synchronized void add(final Runnable runnable) {
            queue.add(runnable);
            notify();
        }
    }

    private static class CountDown {
        private final Object countLock;

        private int count;

        public CountDown(final int count) {
            this.countLock = new Object();
            this.count = count;
        }

        public void await() throws InterruptedException {
            synchronized (countLock) {
                while (count > 0) {
                    countLock.wait();
                }
            }
        }

        public void countDown() {
            synchronized (countLock) {
                --count;
                countLock.notify();
            }
        }
    }
}
