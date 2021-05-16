package info.kgeorgiy.ja.polchinsky.walk.hash;

public final class Hashing {

    public static HashFunction pjw64() {
        return Pjw64Holder.PJW_64;
    }

    private static class Pjw64Holder {
        static final HashFunction PJW_64 = new Pjw64HashFunction();
    }
}
