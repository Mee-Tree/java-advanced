package info.kgeorgiy.ja.polchinsky.walk.hash;

public interface HashFunction {
    long hashBytes(byte[] bytes, int length, long start);

    default long hashBytes(byte[] bytes, int length) {
        return hashBytes(bytes, length, initValue());
    }

    default long initValue() {
        return 0L;
    }
}
