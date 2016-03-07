package org.osgeo.proj4j.datum;

import java.nio.charset.StandardCharsets;

public final class NTV1 {
    private NTV1() {
    }

    private static final byte[] magic1 = "HEADER".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] magic2 = "W GRID".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] magic3 = "TO      NAD83   ".getBytes(StandardCharsets.US_ASCII);

    public static boolean testHeader(byte[] header) {
        return containsAt(magic1, header, 0) &&
               containsAt(magic2, header, 96) &&
               containsAt(magic3, header, 144);
    }

    private static boolean containsAt(byte[] needle, byte[] haystack, int offset) {
        if (needle == null || haystack == null) return false;

        int maxoffset = Math.min(needle.length - 1, haystack.length - offset - 1);
        for (int i = 0; i < maxoffset; i++) {
            if (needle[i] != haystack[offset + i]) return false;
        }
        return true;
    }
}
