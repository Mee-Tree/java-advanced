package info.kgeorgiy.ja.polchinsky.walk;

import info.kgeorgiy.ja.polchinsky.walk.util.ThrowableUtils;

import java.io.IOException;

public class Walk {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Usage:");
            System.err.printf(
                    "\tjava %s input_file output_file",
                    Walk.class.getName());
            System.err.println();
            return;
        }

        try {
            RecursiveWalk.walk(args[0], args[1], 1);
        } catch (IOException | SecurityException e) {
            System.err.println(ThrowableUtils.chainedMessage(e));
        }
    }
}
