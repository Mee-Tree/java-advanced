package info.kgeorgiy.ja.polchinsky.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractHelloClient implements HelloClient {
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("(\\D*)(\\d+)(\\D+)(\\d+)(\\D*)");
    protected static final int SO_TIMEOUT = 200;

    protected String format(final String prefix, final int thread, final int request) {
        return prefix + thread + "_" + request;
    }

    protected static boolean isValid(final String response, final int thread, final int request) {
        final Matcher matcher = RESPONSE_PATTERN.matcher(response);
        return matcher.matches() &&
                Integer.toString(thread).equals(matcher.group(2)) &&
                Integer.toString(request).equals(matcher.group(4));
    }

    protected static void abstractMain(final String[] args, final Supplier<HelloClient> supplier) {
        final Arguments arguments = new Arguments(args);
        final HelloClient helloClient = supplier.get();

        if (arguments.size() != 5) {
            System.err.println("Usage:");
            System.err.printf("\t%s host port prefix threads requests",
                    helloClient.getClass().getName());
            System.err.println();
            return;
        }

        try {
            final String host = arguments.get(0);
            final int port = arguments.getInt(1);
            final String prefix = arguments.get(2);
            final int threads = arguments.getInt(3);
            final int requests = arguments.getInt(3);

            helloClient.run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e) {
            System.err.println("Couldn't parse integer argument: " + e.getMessage());
        }
    }
}
