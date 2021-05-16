package info.kgeorgiy.ja.polchinsky.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {
    private ExecutorService pool;
    private DatagramSocket socket;

    public static void main(final String[] args) {
        final Arguments arguments = new Arguments(args);
        if (arguments.size() != 2) {
            System.err.println("hui");
            return;
        }

        try {
            final int port = arguments.getInt(0);
            final int threads = arguments.getInt(1);

            new HelloUDPServer()
                    .start(port, threads);
        } catch (final NumberFormatException e) {
            System.err.println("jopa");
        }
    }

    @Override
    public void start(final int port, final int threads) {
        this.pool = Executors.newFixedThreadPool(threads);
        try {
            this.socket = new DatagramSocket(port);
            IntStream.range(0, threads).forEach(i -> pool.submit(this::receive));
        } catch (final SocketException e) {
            System.err.println("anal");
        }
    }

    private void receive() {
        try {
            final int size = socket.getReceiveBufferSize();
            final DatagramPacket packet = new DatagramPacket(new byte[size], size);
            final String responseFormat = "Hello, %s";

            while (!socket.isClosed() && !Thread.interrupted()) {
                try {
                    socket.receive(packet);
                    final String request = new String(
                            packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                    final String response = String.format(responseFormat, request);
                    packet.setData(response.getBytes(StandardCharsets.UTF_8));
                    socket.send(packet);
                } catch (final IOException e) {
                    System.err.println("pp");
                }
            }
        } catch (final SocketException e) {
            System.err.println("penis");
        }

    }

    @Override
    public void close() {
        socket.close();
        pool.shutdown();
        while (true) {
            try {
                //noinspection ResultOfMethodCallIgnored
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                break;
            } catch (final InterruptedException ignored) {
                pool.shutdownNow();
            }
        }
    }
}
