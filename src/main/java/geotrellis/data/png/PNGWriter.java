/**
 * PNGWriter.java
 *
 * Copyright (c) 2007 Matthias Mann - www.matthiasmann.de
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Modified by Azavea, 2010-2011.
 */
 
package geotrellis.data.png;
 
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
 
/**
 * A small PNG writer to save RGB data.
 *
 * @author Matthias Mann
 * modified by Erik Osheim
 */
public class PNGWriter {
 
    private static final byte[] SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private static final int IHDR = (int)0x49484452;
    private static final int BKGD = (int)0x624b4744;
    private static final int TRNS = (int)0x74524e53;
    private static final int IDAT = (int)0x49444154;
    private static final int IEND = (int)0x49454E44;
    private static final byte COLOR_TRUECOLOR = 2;
    private static final byte COMPRESSION_DEFLATE = 0;
    private static final byte FILTER_NONE = 0;
    private static final byte INTERLACE_NONE = 0;
    private static final byte PAETH = 4;
 
    /**
     * Writes an image in OpenGL GL_RGB format to an OutputStream.
     *
     * @param os The output stream where the PNG should be written to
     * @param t The Texture object. Contains a ByteBuffer with
     *          compact RGB data (no padding between lines)
     */
    public static void write(OutputStream os, ByteBuffer bb, int width,
                             int height, int bg,
                             boolean transparent) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.write(SIGNATURE);
 
        Chunk cIHDR = new Chunk(IHDR);
        cIHDR.writeInt(width);
        cIHDR.writeInt(height);
        cIHDR.writeByte(8); // 8 bit per component
        cIHDR.writeByte(COLOR_TRUECOLOR);
        cIHDR.writeByte(COMPRESSION_DEFLATE);
        cIHDR.writeByte(FILTER_NONE);
        cIHDR.writeByte(INTERLACE_NONE);
        cIHDR.writeTo(dos);
 
        Chunk cBKGD = new Chunk(BKGD);
        cBKGD.writeByte(0x00);
        cBKGD.writeByte((bg >> 16) & 0xff);
        cBKGD.writeByte(0x00);
        cBKGD.writeByte((bg >> 8) & 0xff);
        cBKGD.writeByte(0x00);
        cBKGD.writeByte((bg) & 0xff);
        cBKGD.writeTo(dos);

        if(transparent) {
            Chunk cTRNS = new Chunk(TRNS);
            cTRNS.writeByte(0x00);
            cTRNS.writeByte((bg >> 16) & 0xff);
            cTRNS.writeByte(0x00);
            cTRNS.writeByte((bg >> 8) & 0xff);
            cTRNS.writeByte(0x00);
            cTRNS.writeByte((bg) & 0xff);
            cTRNS.writeTo(dos);
        }

        Chunk cIDAT = new Chunk(IDAT);
        int level = Deflater.BEST_SPEED;
        DeflaterOutputStream dfos = new DeflaterOutputStream(cIDAT, new Deflater(level));
 
        int lineLen = width * 3;
        byte[] lineOut = new byte[lineLen];
        byte[] curLine = new byte[lineLen];
        byte[] prevLine = new byte[lineLen];
 
        for(int line=0 ; line<height ; line++) {
            bb.position(line * lineLen);
            bb.get(curLine);
 
            lineOut[0] = (byte)(curLine[0] - prevLine[0]);
            lineOut[1] = (byte)(curLine[1] - prevLine[1]);
            lineOut[2] = (byte)(curLine[2] - prevLine[2]);
 
            for(int x=3 ; x<lineLen ; x++) {
                int a = curLine[x-3] & 255;
                int b = prevLine[x] & 255;
                int c = prevLine[x-3] & 255;
                int p = a + b - c;
                int pa = p - a; if(pa < 0) pa = -pa;
                int pb = p - b; if(pb < 0) pb = -pb;
                int pc = p - c; if(pc < 0) pc = -pc;
                if(pa<=pb && pa<=pc)
                    c = a;
                else if(pb<=pc)
                    c = b;
                lineOut[x] = (byte)(curLine[x] - c);
            }
 
            dfos.write(PAETH);
            dfos.write(lineOut);
 
            // swap the line buffers
            byte[] temp = curLine;
            curLine = prevLine;
            prevLine = temp;
        }
 
        dfos.finish();
        try {
            cIDAT.writeTo(dos);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
 
        Chunk cIEND = new Chunk(IEND);
        cIEND.writeTo(dos);
 
        dos.flush();
    }
 
    /**
     * Writes an image in OpenGL GL_RGB format to a File.
     *
     * @param file The file where the PNG should be written to.
                   Existing files will be overwritten.
     * @param t The Texture object. Contains a ByteBuffer with
     *          compact RGB data (no padding between lines)
     */
    public static void write(File file, ByteBuffer bb, int height, int width, int bg, boolean transparent) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            write(fos, bb, width, height, bg, transparent);
        } finally {
            fos.close();
        }
    }
    public static String makeString(ByteBuffer bb, int height, int width, int bg, boolean transparent) throws IOException {
  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos, bb, width, height, bg, transparent);
        return baos.toString();
    }

    public static byte[] makeByteArray(ByteBuffer bb, int height, int width, int bg, boolean transparent) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos, bb, width, height, bg, transparent);
        return baos.toByteArray();
    }
 
    static class Chunk extends DataOutputStream {
        final CRC32 crc;
        final ByteArrayOutputStream baos;
 
        Chunk(int chunkType) throws IOException {
            this(chunkType, new ByteArrayOutputStream(), new CRC32());
        }
        private Chunk(int chunkType, ByteArrayOutputStream baos,
                      CRC32 crc) throws IOException {
            super(new CheckedOutputStream(baos, crc));
            this.crc = crc;
            this.baos = baos;
 
            writeInt(chunkType);
        }
 
        public void writeTo(DataOutputStream out) throws IOException {
            flush();
            out.writeInt(baos.size() - 4);
            baos.writeTo(out);
            out.writeInt((int)crc.getValue());
        }
    }
}
