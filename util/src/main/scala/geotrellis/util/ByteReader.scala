package geotrellis.util

import java.nio.{Buffer, ByteBuffer, ByteOrder}
import scala.language.implicitConversions

trait ByteReader {
  def byteBuffer: ByteBuffer

  /*
  val array: Array[Byte]
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
  */
}


object ByteReader {
  implicit def byteBuffer2ByteReader(buffer: ByteBuffer): ByteReader = {
    new ByteReader() {
      def byteBuffer = buffer
    }
  }
    /*
    new ByteReader() {
      override val array = byteBuffer.array
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
    */
  
  implicit def toByteBuffer(br: ByteReader): ByteBuffer =
    br.byteBuffer
}
