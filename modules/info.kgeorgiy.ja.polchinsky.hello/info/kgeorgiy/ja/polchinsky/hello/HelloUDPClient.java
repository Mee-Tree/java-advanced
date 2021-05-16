package info.kgeorgiy.ja.polchinsky.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    public static void main(final String[] args) {
        final Arguments arguments = new Arguments(args);
        if (arguments.size() != 5) {
            System.err.println("hui");
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
            System.err.println("jopa");
        }
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress address = new InetSocketAddress(host, port);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        IntStream.range(0, threads).forEach(i -> {
            pool.submit(() -> send(address, prefix, i, requests));
        });

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
            socket.setSoTimeout(0xDEAD / 1000);
            final DatagramPacket packet = new DatagramPacket(new byte[0], 0, address);

            for (int request = 0; request < requests; request++) {
                final String data = prefix + thread + "_" + request;
                System.out.println(data);

                while (!Thread.interrupted()) {
                    try {
                        packet.setData(data.getBytes(StandardCharsets.UTF_8));
                        socket.send(packet);

                        packet.setData(new byte[socket.getReceiveBufferSize()]);
                        socket.receive(packet);
                        final String response = new String(
                                packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

                        System.out.println(response);
                        break;
                    } catch (final IOException e) {
                        System.err.println("pp");
                    }
                }

            }
        } catch (final SocketException e) {
            System.err.println("penis");
        }
    }
}
