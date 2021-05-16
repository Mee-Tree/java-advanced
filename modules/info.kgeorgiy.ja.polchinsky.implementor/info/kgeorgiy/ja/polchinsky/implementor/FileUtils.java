package info.kgeorgiy.ja.polchinsky.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * This class consists exclusively of static utility methods that operate on or return paths.
 *
 * @author Dmitry Polchinsky
 * @see Path
 */
public final class FileUtils {

    /**
     * An instance of {@link SimpleFileVisitor}
     * which deletes the file tree using {@link Files#delete(Path)}.
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }

            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Default constructor. Made private to prevent instantiation of utility class.
     */
    private FileUtils() {
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     *
     * @param path the directory to create
     * @throws ImplerException if an error occurs during creation
     */
    public static void createDirectories(final Path path) throws ImplerException {
        final Path parent = path.toAbsolutePath().getParent();

        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                throw new ImplerException("Cannot create directories", e);
            }
        }
    }

    /**
     * Creates a new temporary directory in the specified directory.
     *
     * @param dir the path to directory in which to create the directory
     * @return the path to the newly created directory
     * @throws ImplerException if an error occurs during creation
     */
    public static Path createTempDirectory(final Path dir) throws ImplerException {
        try {
            return Files.createTempDirectory(dir, "jar-temp");
        } catch (final IOException e) {
            throw new ImplerException("Could not create temporary directory", e);
        }
    }

    /**
     * Deletes the specified directory and all its contents,
     * including any subdirectories and files.
     * <p>
     * Calls {@link Files#walkFileTree(Path, FileVisitor)} with {@code dir} and {@link #DELETE_VISITOR}.
     *
     * @param dir the path to directory to be deleted
     * @throws ImplerException if an error occurs during deletion
     */
    public static void deleteDirectory(final Path dir) throws ImplerException {
        try {
            Files.walkFileTree(dir, DELETE_VISITOR);
        } catch (final IOException e) {
            throw new ImplerException("Could not clean temporary directory", e);
        }
    }
}
