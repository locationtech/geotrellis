package geotrellis.util

import java.nio.{Buffer, ByteBuffer}
import scala.language.implicitConversions

trait ByteReader {
  def get: Byte
  def getChar: Char
  def getShort: Short
  def getInt: Int
  def getFloat: Float
  def getDouble: Double
  def position: Int
  def position(i: Int): Buffer
  def getByteBuffer: ByteBuffer
}

object ByteReader {
  implicit def byteBuffer2ByteReader(byteBuffer: ByteBuffer): ByteReader = {
    new ByteReader() {
      def get = byteBuffer.get
      def getChar = byteBuffer.getChar
      def getShort = byteBuffer.getShort
      def getInt = byteBuffer.getInt
      def getFloat = byteBuffer.getFloat
      def getDouble = byteBuffer.getDouble
      def position: Int = byteBuffer.position
      def position(i: Int): Buffer = byteBuffer.position(i)
      def getByteBuffer = byteBuffer
    }
  }
  implicit def toByteBuffer(br: ByteReader): ByteBuffer = br.getByteBuffer
}
