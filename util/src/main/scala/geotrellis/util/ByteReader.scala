package geotrellis.util

import java.nio.{Buffer, ByteBuffer}
import scala.language.implicitConversions

/**
 * This trait is a characteristic of instances that can retrieve bytes from some
 * source.
 */
trait ByteReader {
  def position: Long
  def position(i: Long): Buffer

  def get: Byte
  def getChar: Char
  def getShort: Short
  def getInt: Int
  def getFloat: Float
  def getDouble: Double
  def getLong: Long

  def getByteBuffer: ByteBuffer
}

/**
 * The companion object of [[ByteReader]]. This object contains implicit conversion
 * to and from ByteBuffers and ByteReaders.
 */
object ByteReader {
  implicit def byteBuffer2ByteReader(byteBuffer: ByteBuffer): ByteReader = {
    new ByteReader() {
      def position: Long = byteBuffer.position.toLong
      def position(i: Long): Buffer = byteBuffer.position(i.toInt)

      def get = byteBuffer.get
      def getChar = byteBuffer.getChar
      def getShort = byteBuffer.getShort
      def getInt = byteBuffer.getInt
      def getFloat = byteBuffer.getFloat
      def getDouble = byteBuffer.getDouble
      def getLong = byteBuffer.getLong

      def getByteBuffer = byteBuffer
    }
  }

  implicit def toByteBuffer(br: ByteReader): ByteBuffer =
    br.getByteBuffer
}
