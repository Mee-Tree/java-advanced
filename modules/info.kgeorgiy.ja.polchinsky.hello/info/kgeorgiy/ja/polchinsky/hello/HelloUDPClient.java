package info.kgeorgiy.ja.polchinsky.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private static final int SO_TIMEOUT = 10;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static void main(final String[] args) {
        final Arguments arguments = new Arguments(args);
        if (arguments.size() != 5) {
            System.err.println("Usage:");
            System.err.printf("\t%s host port prefix threads requests",
                    HelloUDPClient.class.getName());
            System.err.println();
            return;
        }

        try {
            final String host = arguments.get(0);
            final int port = arguments.getInt(1);
            final String prefix = arguments.get(2);
            final int threads = arguments.getInt(3);
            final int requests = arguments.getInt(3);

            new HelloUDPClient()
                    .run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e) {
            System.err.println("Couldn't parse integer argument: " + e.getMessage());
        }
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress address = new InetSocketAddress(host, port);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        IntStream.range(0, threads).forEach(i ->
                pool.submit(() -> send(address, prefix, i, requests)));

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

    private void send(final SocketAddress address, final String prefix, final int thread, final int requests) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SO_TIMEOUT);
            final DatagramPacket packet = new DatagramPacket(new byte[0], 0, address);

            for (int request = 0; request < requests; request++) {
                final String data = prefix + thread + "_" + request;
                System.out.println(data);

                while (!Thread.interrupted()) {
                    try {
                        packet.setData(data.getBytes(CHARSET));
                        socket.send(packet);

                        packet.setData(new byte[socket.getReceiveBufferSize()]);
                        socket.receive(packet);
                        final String response = new String(
                                packet.getData(), packet.getOffset(), packet.getLength(), CHARSET);

                        System.out.println(response);
                        break;
                    } catch (final SocketTimeoutException e) {
                        System.err.println("Socket has timed out: " + e.getMessage());
                    } catch (final IOException e) {
                        System.err.println("An I/O error has occurred during data transfer: " + e.getMessage());
                    }
                }
            }
        } catch (final SocketException e) {
            System.err.println("Couldn't create socket: " + e.getMessage());
        }
    }
}
