package info.kgeorgiy.ja.polchinsky.hello;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class Utils {
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private Utils() {
    }

    public static ByteBuffer encode(final String data) {
        return ByteBuffer.wrap(data.getBytes(CHARSET));
    }

    public static String decode(final ByteBuffer buffer) {
        return CHARSET.decode(buffer.flip()).toString();
    }

    public static DatagramPacket createPacket(final int size, final SocketAddress address) {
        if (address == null) {
            return new DatagramPacket(new byte[size], size);
        }
        return new DatagramPacket(new byte[size], size, address);
    }

    public static void send(final DatagramSocket socket, final DatagramPacket packet, final String data) throws IOException {
        packet.setData(data.getBytes(CHARSET));
        socket.send(packet);
    }

    public static String receive(final DatagramSocket socket, final DatagramPacket packet) throws IOException {
        packet.setData(new byte[socket.getReceiveBufferSize()]);
        socket.receive(packet);
        return new String(
                packet.getData(), packet.getOffset(), packet.getLength(), CHARSET);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static DatagramChannel createChannel(final Selector selector,
                                                final int ops,
                                                final Object attachment,
                                                final ThrowingConsumer<DatagramChannel> connector) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        connector.accept(channel);
        channel.register(selector, ops, attachment);
        return channel;
    }

    public static void forEachSelected(final Set<SelectionKey> selectionKeys,
                                       final ThrowingConsumer<SelectionKey> write,
                                       final ThrowingConsumer<SelectionKey> read) {
        for (final Iterator<SelectionKey> i = selectionKeys.iterator(); i.hasNext(); ) {
            final SelectionKey key = i.next();
            try {
                if (key.isWritable()) {
                    write.accept(key);
                }

                if (key.isReadable()) {
                    read.accept(key);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            } finally {
                i.remove();
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean tryClose(final Closeable closeable) {
        try {
            closeable.close();
            return true;
        } catch (final IOException ignored) {
            return false;
        }
    }

    public static void shutdown(final ExecutorService service) {
        service.shutdown();
        while (true) {
            try {
                //noinspection ResultOfMethodCallIgnored
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                break;
            } catch (final InterruptedException ignored) {
                service.shutdownNow();
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }
}
