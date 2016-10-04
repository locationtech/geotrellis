package geotrellis.util

import java.nio.{Buffer, ByteBuffer}
import scala.language.implicitConversions

trait ByteReader {
  def length: Int

  def isContained(point: Int): Boolean

  def position: Int
  def position(i: Int): Buffer

  def get: Byte
  def getChar: Char
  def getShort: Short
  def getInt: Int
  def getFloat: Float
  def getDouble: Double
  def getLong: Long
  
  def getByteBuffer: ByteBuffer
}

object ByteReader {
  implicit def byteBuffer2ByteReader(byteBuffer: ByteBuffer): ByteReader = {
    new ByteReader() {
      def length = byteBuffer.capacity

      def isContained(point: Int) =
        if (point >= 0 && point <= byteBuffer.capacity) true else false

      def position: Int = byteBuffer.position
      def position(i: Int): Buffer = byteBuffer.position(i)

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
  implicit def toByteBuffer(br: ByteReader): ByteBuffer = br.getByteBuffer
}
