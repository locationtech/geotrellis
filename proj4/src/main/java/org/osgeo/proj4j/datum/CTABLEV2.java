/*
 * Copyright 2016 Martin Davis, Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgeo.proj4j.datum;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.osgeo.proj4j.util.IntPolarCoordinate;
import org.osgeo.proj4j.util.FloatPolarCoordinate;
import org.osgeo.proj4j.util.PolarCoordinate;

public final class CTABLEV2 {
    private CTABLEV2() {
    }

    private static final byte[] magic = "CTABLE V2".getBytes(StandardCharsets.US_ASCII);

    public static boolean testHeader(byte[] header) {
        return containsAt(magic, header, 0);
    }

    public static Grid.ConversionTable init(DataInputStream definition) throws IOException {
        byte[] header = new byte[160];
        definition.readFully(header);
        if (!containsAt(magic, header, 0)) throw new Error("Not a CTABLE V2 file");
        byte[] id_bytes = Arrays.copyOfRange(header, 16, 16 + 80);
        PolarCoordinate ll = new PolarCoordinate(doubleFromBytes(header, 96), doubleFromBytes(header, 104));
        PolarCoordinate del = new PolarCoordinate(doubleFromBytes(header, 112), doubleFromBytes(header, 120));
        IntPolarCoordinate lim = new IntPolarCoordinate(intFromBytes(header, 128), intFromBytes(header, 132));

        // Minimal validation to detect corrupt structure
        if (lim.lam < 1 || lim.lam > 100000 || lim.phi < 1 || lim.phi > 100000) {
            throw new Error("Grid position counts outside of acceptable parameters for CTABLE file " + lim);
        }

        int nullPosition = 0;
        while (nullPosition < id_bytes.length && id_bytes[nullPosition] != 0) nullPosition++;
        Grid.ConversionTable table = new Grid.ConversionTable();
        table.id = new String(id_bytes, 0, nullPosition, StandardCharsets.US_ASCII).trim();
        table.ll = ll;
        table.del = del;
        table.lim = lim;
        return table;
    }

    public static void load(DataInputStream definition, Grid grid) throws IOException {
        Grid.ConversionTable table = grid.table;
        int entryCount = table.lim.lam * table.lim.phi;
        FloatPolarCoordinate[] cvs = new FloatPolarCoordinate[entryCount];
        byte[] buff = new byte[8];
        definition.skipBytes(160);
        for (int i = 0; i < entryCount; i++) {
            definition.readFully(buff);
            cvs[i] = new FloatPolarCoordinate(floatFromBytes(buff, 0), floatFromBytes(buff, 4));
        }
        table.cvs = cvs;
    }

    private static boolean containsAt(byte[] needle, byte[] haystack, int offset) {
        if (needle == null || haystack == null) return false;

        int maxoffset = Math.min(needle.length - 1, haystack.length - offset - 1);
        for (int i = 0; i < maxoffset; i++) {
            if (needle[i] != haystack[offset + i]) return false;
        }

        return true;
    }

    private static double doubleFromBytes(byte[] b, int offset) {
        return ByteBuffer.wrap(b, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    private static int intFromBytes(byte[] b, int offset) {
        return ByteBuffer.wrap(b, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static float floatFromBytes(byte[] b, int offset) {
        return ByteBuffer.wrap(b, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }
}
