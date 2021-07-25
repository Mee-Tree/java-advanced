package info.kgeorgiy.ja.polchinsky.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer extends AbstractHelloServer implements HelloServer {
    private Selector selector;

    private ExecutorService pool;
    private Thread thread;

    public static void main(final String[] args) {
        abstractMain(args, HelloUDPNonblockingServer::new);
    }

    @Override
    public void start(final int port, final int threads) {
        try {
            final SocketAddress address = new InetSocketAddress(port);
            selector = Selector.open();
            pool = Executors.newFixedThreadPool(threads);

            Utils.createChannel(selector, SelectionKey.OP_READ,
                    new Attachment(),
                    channel -> channel.bind(address));

            thread = new Thread(this::run);
            thread.start();
        } catch (final IOException | UncheckedIOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            close();
        }
    }

    private void run() {
        try {
            while (!Thread.interrupted() && selector.isOpen()) {
                selector.select();

                Utils.forEachSelected(selector.selectedKeys(),
                        this::write,
                        this::read);
            }
        } catch (final ClosedChannelException | ClosedSelectorException ignored) {
        } catch (final IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    private void write(final SelectionKey key) throws IOException {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Attachment attachment = (Attachment) key.attachment();
        final Attachment.Request request = attachment.pollRequest();

        if (request == null) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        channel.send(Utils.encode(request.getData()), request.getAddress());

        key.interestOps(SelectionKey.OP_READ);
        if (!attachment.isEmpty()) {
            key.interestOpsOr(SelectionKey.OP_WRITE);
        }
    }

    private void read(final SelectionKey key) throws IOException {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Attachment attachment = (Attachment) key.attachment();
        final ByteBuffer buffer = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
        final SocketAddress address = channel.receive(buffer);

        pool.submit(() -> {
            final String request = Utils.decode(buffer);
            attachment.addRequest(format(request), address);
            key.interestOps(SelectionKey.OP_WRITE);
            selector.wakeup();
        });
    }

    @Override
    public void close() {
        thread.interrupt();
        selector.keys().stream()
                .map(SelectionKey::channel)
                .forEach(Utils::tryClose);

        Utils.tryClose(selector);
        Utils.shutdown(pool);
    }

    private static class Attachment {
        private final Queue<Request> requests;

        public Attachment() {
            this.requests = new ConcurrentLinkedQueue<>();
        }

        public void addRequest(final String data, final SocketAddress address) {
            requests.offer(new Request(data, address));
        }

        public Request pollRequest() {
            return requests.poll();
        }

        public boolean isEmpty() {
            return requests.isEmpty();
        }

        private static class Request {
            private final String data;
            private final SocketAddress address;

            public Request(final String data, final SocketAddress address) {
                this.data = data;
                this.address = address;
            }

            public String getData() {
                return data;
            }

            public SocketAddress getAddress() {
                return address;
            }
        }
    }
}
