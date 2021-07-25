package info.kgeorgiy.ja.polchinsky.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class HelloUDPNonblockingClient extends AbstractHelloClient {

    public static void main(final String[] args) {
        abstractMain(args, HelloUDPNonblockingClient::new);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress address = new InetSocketAddress(host, port);
        Selector selector = null;

        try {
            selector = Selector.open();
            for (int thread = 0; thread < threads; thread++) {
                Utils.createChannel(selector, SelectionKey.OP_WRITE,
                        new Attachment(thread, requests),
                        channel -> channel.connect(address));
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                selector.select(SO_TIMEOUT);

                final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                if (selectionKeys.isEmpty()) {
                    selector.keys().forEach(
                            k -> k.interestOps(SelectionKey.OP_WRITE));
                }

                Utils.forEachSelected(selectionKeys,
                        key -> write(prefix, key),
                        this::read);
            }
        } catch (final UncheckedIOException | IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
        } finally {
            if (selector != null) {
                selector.keys().stream()
                        .map(SelectionKey::channel)
                        .forEach(Utils::tryClose);

                Utils.tryClose(selector);
            }
        }
    }

    private void read(final SelectionKey key) throws IOException {
        final Attachment attachment = (Attachment) key.attachment();
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final ByteBuffer buffer = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());

        channel.read(buffer);
        final String response = Utils.decode(buffer);
        key.interestOps(SelectionKey.OP_WRITE);

        if (isValid(response, attachment.getThread(), attachment.getRequest())) {
            System.out.println(response);
            if (!attachment.next()) {
                channel.close();
            }
        }
    }

    private void write(final String prefix, final SelectionKey key) throws IOException {
        final Attachment request = (Attachment) key.attachment();
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final String data = format(prefix, request.getThread(), request.getRequest());
        System.out.println(data);

        channel.write(Utils.encode(data));
        key.interestOps(SelectionKey.OP_READ);
    }

    private static class Attachment {
        private final int thread;
        private final int requests;

        private int request;

        public Attachment(final int thread, final int requests) {
            this.thread = thread;
            this.requests = requests;
            this.request = 0;
        }

        public boolean next() {
            return ++request < requests;
        }

        public int getThread() {
            return thread;
        }

        public int getRequest() {
            return request;
        }
    }
}
