package info.kgeorgiy.ja.polchinsky.walk.util;

public final class ThrowableUtils {
    private ThrowableUtils() { }

    public static String toSimpleString(Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        return message == null ? name : name + ": " + message;
    }

    public static String chainedMessage(Throwable throwable) {
        StringBuilder sb = new StringBuilder(toSimpleString(throwable));
        while ((throwable = throwable.getCause()) != null) {
            sb.append(System.lineSeparator())
                    .append("caused by ")
                    .append(toSimpleString(throwable));
        }
        return sb.toString();
    }
}
