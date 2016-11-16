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
import static org.osgeo.proj4j.util.ProjectionMath.DTR;

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

    public static Grid.ConversionTable init(DataInputStream definition) throws IOException {
        byte[] header = new byte[160];
        definition.readFully(header);
        if (!testHeader(header)) throw new Error("Not a NTV1 file");

        // Minimal validation to detect corrupt structure
        int recordCount = intFromBytes(header, 8);
        if (recordCount != 12) {
            throw new Error(String.format("NTv1 grid shift file has wrong record count, corrupt? $0%08X $0", recordCount));
        }

        Grid.ConversionTable table = new Grid.ConversionTable();
        table.id = "NTv1 Grid Shift File";
        table.ll = new PolarCoordinate(- doubleFromBytes(header, 72), doubleFromBytes(header, 24));
        PolarCoordinate ur = new PolarCoordinate(- doubleFromBytes(header, 56), doubleFromBytes(header, 40));
        table.del = new PolarCoordinate(doubleFromBytes(header, 104), doubleFromBytes(header, 88));
        table.lim = new IntPolarCoordinate(
                (int) (Math.abs(ur.lam - table.ll.lam) / table.del.lam + 0.5) + 1,
                (int) (Math.abs(ur.phi - table.ll.phi) / table.del.phi + 0.5) + 1);
        table.ll.lam *= DTR;
        table.ll.phi *= DTR;
        table.del.lam *= DTR;
        table.del.phi *= DTR;
        return table;
    }

    public static void load(DataInputStream definition, Grid grid) throws IOException {
        definition.skip(176);
        double[] row_buff = new double[grid.table.lim.lam * 2];
        FloatPolarCoordinate[] tmp_cvs =
            new FloatPolarCoordinate[grid.table.lim.lam * grid.table.lim.phi];

        for (int row = 0; row < grid.table.lim.phi; row++) {
            byte[] byteBuff = new byte[8 * row_buff.length];
            definition.readFully(byteBuff);
            ByteBuffer.wrap(byteBuff).order(ByteOrder.BIG_ENDIAN).asDoubleBuffer().get(row_buff);
            for (int i = 0; i < grid.table.lim.lam; i++) {
                tmp_cvs[row * grid.table.lim.lam + grid.table.lim.lam - i - 1] =
                    new FloatPolarCoordinate(
                            (float) (row_buff[2 * i] * Math.PI / 180.0 / 3600.0),
                            (float) (row_buff[2 * i + 1] * Math.PI / 180.0 / 3600.0));
            }
        }
        grid.table.cvs = tmp_cvs;

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
        return ByteBuffer.wrap(b, offset, 8).order(ByteOrder.BIG_ENDIAN).getDouble();
    }

    private static int intFromBytes(byte[] b, int offset) {
        return ByteBuffer.wrap(b, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }
}
