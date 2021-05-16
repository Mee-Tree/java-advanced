package info.kgeorgiy.ja.polchinsky.hello;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class Arguments {
    private final List<String> arguments;

    public Arguments(final String[] args) {
        Objects.requireNonNull(args);
        for (final String arg : args) {
            Objects.requireNonNull(arg);
        }

        this.arguments = Arrays.asList(args);
    }

    public int size() {
        return arguments.size();
    }

    public String get(final int index) {
        return arguments.get(index);
    }

    public Integer getInt(final int index) throws NumberFormatException {
        return Integer.parseInt(get(index));
    }
}
