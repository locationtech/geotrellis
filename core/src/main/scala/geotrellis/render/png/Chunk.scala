package geotrellis.render.png

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

import scala.math.abs

import Util._

final class Chunk(chunkType:Int) {
  val baos = new ByteArrayOutputStream()
  val crc = new CRC32()

  val cos = new CheckedOutputStream(baos, crc)
  writeInt(chunkType)

  def writeInt(i:Int) {
    cos.write(byte(i >> 24))
    cos.write(byte(i >> 16))
    cos.write(byte(i >> 8))
    cos.write(byte(i))
  }

  def writeByte(b:Byte) {
    cos.write(b)
  }

  def writeTo(out:DataOutputStream) {
    cos.flush()
    out.writeInt(baos.size() - 4)
    baos.writeTo(out)
    out.writeInt(crc.getValue().asInstanceOf[Int])
  }
}
