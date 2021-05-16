/**
 * IterativeParallelism and ParallelMapper solutions
 * for <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author Dmitry Polchinsky
 */
module info.kgeorgiy.ja.polchinsky.concurrent {
    requires transitive info.kgeorgiy.java.advanced.concurrent;
    requires transitive info.kgeorgiy.java.advanced.mapper;

    exports info.kgeorgiy.ja.polchinsky.concurrent;
}