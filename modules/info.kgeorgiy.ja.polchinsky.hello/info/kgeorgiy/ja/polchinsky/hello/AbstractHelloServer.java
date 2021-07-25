package info.kgeorgiy.ja.polchinsky.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.function.Supplier;

public abstract class AbstractHelloServer implements HelloServer {

    protected String format(final String request) {
        return "Hello, " + request;
    }

    protected static void abstractMain(final String[] args, final Supplier<HelloServer> supplier) {
        final Arguments arguments = new Arguments(args);
        final HelloServer helloServer = supplier.get();

        if (arguments.size() != 2) {
            System.err.println("Usage:");
            System.err.printf("\t%s port threads",
                    helloServer.getClass().getName());
            System.err.println();
            return;
        }

        try {
            final int port = arguments.getInt(0);
            final int threads = arguments.getInt(1);

            helloServer.start(port, threads);
        } catch (final NumberFormatException e) {
            System.err.println("Couldn't parse integer argument: " + e.getMessage());
        }
    }
}
