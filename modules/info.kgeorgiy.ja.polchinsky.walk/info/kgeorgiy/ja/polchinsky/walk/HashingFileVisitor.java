package info.kgeorgiy.ja.polchinsky.walk;

import info.kgeorgiy.ja.polchinsky.walk.hash.HashFunction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashingFileVisitor extends SimpleFileVisitor<Path> {
    private static final int BUFFER_SIZE = 8192;
    private static final long ERROR_HASH = 0L;

    private final HashFunction hashFunction;
    private final BufferedWriter writer;

    public HashingFileVisitor(HashFunction hashFunction, BufferedWriter writer) {
        this.hashFunction = hashFunction;
        this.writer = writer;
    }

    private FileVisitResult write(long hash, String filename) throws IOException {
        writer.write(String.format("%016x %s", hash, filename));
        writer.newLine();
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        long hash = hashFunction.initValue();
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] bytes = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytes)) >= 0) {
                hash = hashFunction.hashBytes(bytes, bytesRead, hash);
            }
        }
        return write(hash, file.toString());
    }

    public FileVisitResult visitFileFailed(String filename) throws IOException {
        return write(ERROR_HASH, filename);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return visitFileFailed(file.toString());
    }
}
