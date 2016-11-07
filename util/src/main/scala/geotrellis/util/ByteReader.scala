package geotrellis.util

import java.nio.{Buffer, ByteBuffer, ByteOrder}
import scala.language.implicitConversions

/**
 * This trait is a characteristic of instances that can retrieve bytes from some
 * source.
 */
trait ByteReader {
  def position: Long
  def position(i: Long): ByteReader

  def getBytes(length: Int): Array[Byte]
  def get: Byte
  def getChar: Char
  def getShort: Short
  def getInt: Int
  def getFloat: Float
  def getDouble: Double
  def getLong: Long

  def order: ByteOrder
  def order(byteOrder: ByteOrder): Unit
}

/**
 * The companion object of [[ByteReader]]. This object contains implicit conversion
 * to and from ByteBuffers and ByteReaders.
 */
object ByteReader {
  implicit def byteBuffer2ByteReader(byteBuffer: ByteBuffer): ByteReader = {
    new ByteReader() {
      def position: Long = byteBuffer.position.toLong
      def position(i: Long): ByteReader = { byteBuffer.position(i.toInt) ; this }

      def getBytes(length: Int): Array[Byte] = {
        val arr = Array.ofDim[Byte](length)
        var i = 0
        while(i < length) {
          arr(i) = get
          i += 1
        }

        arr
      }

      def get = byteBuffer.get
      def getChar = byteBuffer.getChar
      def getShort = byteBuffer.getShort
      def getInt = byteBuffer.getInt
      def getFloat = byteBuffer.getFloat
      def getDouble = byteBuffer.getDouble
      def getLong = byteBuffer.getLong

      def order = byteBuffer.order()
      def order(byteOrder: ByteOrder): Unit =
        byteBuffer.order(byteOrder)
    }
  }
}
