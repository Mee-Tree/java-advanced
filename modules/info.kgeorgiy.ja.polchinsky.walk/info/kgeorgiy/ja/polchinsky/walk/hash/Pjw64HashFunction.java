package info.kgeorgiy.ja.polchinsky.walk.hash;

class Pjw64HashFunction implements HashFunction {
    private static final int BITS = Long.SIZE;
    private static final int THREE_QUARTERS = BITS * 3 / 4;
    private static final int ONE_EIGHTH = BITS / 8;
    private static final long TOP_BITS = 0xff00_0000_0000_0000L;

    @Override
    public long hashBytes(byte[] bytes, int length, long start) {
        for (int i = 0; i < length; ++i) {
            start = (start << ONE_EIGHTH) + (bytes[i] & 0xff);
            long high = start & TOP_BITS;
            if (high != 0) {
                start ^= high >> THREE_QUARTERS;
                start &= ~high;
            }
        }
        return start;
    }
}
