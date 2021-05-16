package info.kgeorgiy.ja.polchinsky.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final Map<String, Semaphore> hosts;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        this.hosts = new ConcurrentHashMap<>();
    }

    public static void main(final String[] args) {
        Objects.requireNonNull(args);
        if (args.length == 0) {
            System.err.println("Usage:");
            System.err.printf("\t%s url [depth [downloads [extractors [perHost]]]]",
                    WebCrawler.class.getName());
            System.err.println();
            return;
        }

        try {
            final String url = args[0];
            final int depth = getOrDefault(args, 1);
            final int downloads = getOrDefault(args, 2);
            final int extractors = getOrDefault(args, 3);
            final int perHost = getOrDefault(args, 4);

            final Downloader downloader = new CachingDownloader();
            try (final Crawler crawler = new WebCrawler(downloader, downloads, extractors, perHost)) {
                crawler.download(url, depth);
            }
        } catch (final NumberFormatException e) {
            System.err.println("Couldn't parse number: " + e.getMessage());
        } catch (final IOException e) {
            System.err.println("Couldn't download page: " + e.getMessage());
        }
    }

    private static int getOrDefault(final String[] args, final int index) {
        return index < args.length ? Integer.parseInt(args[index]) : 1;
    }

    @Override
    public Result download(final String url, final int depth) {
        return new BreadthFirstExtractor(url).downloadRecursively(depth);
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void close() {
        downloaders.shutdown();
        extractors.shutdown();
        try {
            final long timeout = Long.MAX_VALUE;
            downloaders.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            extractors.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException ignored) {
            downloaders.shutdownNow();
            extractors.shutdownNow();
        }
    }

    class BreadthFirstExtractor {
        private final Queue<String> queue;
        private final Map<String, IOException> failed;
        private final Set<String> downloaded;
        private final Set<String> extracted;
        private final Phaser phaser;

        public BreadthFirstExtractor(final String url) {
            this.queue = new ConcurrentLinkedQueue<>();
            this.failed = new ConcurrentHashMap<>();
            this.downloaded = ConcurrentHashMap.newKeySet();
            this.extracted = ConcurrentHashMap.newKeySet();
            this.phaser = new Phaser(1);
            queue.add(url);
        }

        public Result downloadRecursively(final int depth) {
            for (int i = 1; i <= depth; ++i) {
                final List<String> previous = List.copyOf(queue);
                final boolean last = i == depth;
                queue.clear();

                previous.parallelStream()
                        .filter(extracted::add)
                        .forEach(url -> download(url, last));
                phaser.arriveAndAwaitAdvance();
            }

            return new Result(List.copyOf(downloaded), failed);
        }

        private void download(final String url, final boolean last) {
            final String host;
            try {
                host = URLUtils.getHost(url);
            } catch (final MalformedURLException e) {
                failed.put(url, e);
                return;
            }

            final Semaphore limiter = hosts.computeIfAbsent(host, h -> new Semaphore(perHost));
            phaser.register();
            downloaders.submit(() -> {
                try {
                    limiter.acquire();
                    final Document document = downloader.download(url);
                    downloaded.add(url);

                    if (!last) {
                        extract(document);
                    }
                } catch (final IOException e) {
                    failed.put(url, e);
                } catch (final InterruptedException ignored) {
                } finally {
                    limiter.release();
                    phaser.arriveAndDeregister();
                }
            });
        }

        private void extract(final Document document) {
            phaser.register();
            extractors.submit(() -> {
                try {
                    queue.addAll(document.extractLinks());
                } catch (final IOException ignored) {
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
    }
}
