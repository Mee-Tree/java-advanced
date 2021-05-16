package info.kgeorgiy.ja.polchinsky.walk;

import info.kgeorgiy.ja.polchinsky.walk.hash.Hashing;
import info.kgeorgiy.ja.polchinsky.walk.util.ThrowableUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;

public class RecursiveWalk {
    private static final int MAX_DEPTH = Integer.MAX_VALUE;

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Usage:");
            System.err.printf(
                    "\tjava %s input_file output_file",
                    RecursiveWalk.class.getName());
            System.err.println();
//            System.exit(1);
            return;
        }

        try {
            walk(args[0], args[1]);
        } catch (IOException | SecurityException e) {
            System.err.println(ThrowableUtils.chainedMessage(e));
        }
    }

    public static void walk(String inputFilename, String outputFilename) throws IOException, SecurityException {
        walk(inputFilename, outputFilename, MAX_DEPTH);
    }

    public static void walk(String inputFilename, String outputFilename, int depth) throws IOException, SecurityException {
        Path inputFile = tryGetPath(inputFilename);
        Path outputFile = tryGetPath(outputFilename);

        if (Files.notExists(inputFile)) {
            throw new IOException("Input file does not exist");
        } else if (!Files.isRegularFile(inputFile)) {
            throw new IOException("Input file is not a file");
        }

        if (outputFile.getParent() != null) {
            try {
                Files.createDirectories(outputFile.getParent());
            } catch (IOException e) {
                throw new IOException("Output file cannot be created", e);
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                HashingFileVisitor fileVisitor = new HashingFileVisitor(
                        Hashing.pjw64(), writer);
                String filename;

                while ((filename = reader.readLine()) != null) {
                    try {
                        Files.walkFileTree(
                                Path.of(filename),
                                EnumSet.noneOf(FileVisitOption.class),
                                depth,
                                fileVisitor);
                    } catch (InvalidPathException | SecurityException ignored) {
                        fileVisitor.visitFileFailed(filename);
                    }
                }
            } catch (IOException e) {
                throw new IOException(
                        "An I/O error occurred during creating/opening output file: " + outputFilename, e);
            }
        } catch (IOException e) {
            throw new IOException(
                    "An I/O error occurred during opening input file: " + inputFilename, e);
        }
    }

    private static Path tryGetPath(String filename) throws IOException {
        try {
            return Path.of(filename);
        } catch (InvalidPathException e) {
            throw new IOException(filename + " is not a valid path", e);
        }
    }
}
