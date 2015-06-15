package geotrellis.raster.io.geotiff.writer

import spire.syntax.cfor._

sealed trait ToBytes {
  def apply(s: String): Array[Byte] = s.getBytes :+ 0x00.toByte

  def apply(i: Int): Array[Byte]
  def apply(i: Short): Array[Byte]
  def apply(i: Long): Array[Byte]

  def apply(f: Float): Array[Byte]
  def apply(d: Double): Array[Byte]

  def apply(arr: Array[Short]): Array[Byte] = {
    val result = Array.ofDim[Byte](arr.length * 2)
    var resultIndex = 0
    cfor(0)(_ < arr.length, _ + 1) { i =>
      val bytes = apply(arr(i))
      System.arraycopy(bytes, 0, result, resultIndex, bytes.length)
      resultIndex += bytes.length
    }
    result
  }

  def apply(arr: Array[Int]): Array[Byte] = {
    val result = Array.ofDim[Byte](arr.length * 4)
    var resultIndex = 0
    cfor(0)(_ < arr.length, _ + 1) { i =>
      val bytes = apply(arr(i))
      System.arraycopy(bytes, 0, result, resultIndex, bytes.length)
      resultIndex += bytes.length
    }
    result
  }

  def apply(arr: Array[Double]): Array[Byte] = {
    val result = Array.ofDim[Byte](arr.length * 8)
    var resultIndex = 0
    cfor(0)(_ < arr.length, _ + 1) { i =>
      val bytes = apply(arr(i))
      System.arraycopy(bytes, 0, result, resultIndex, bytes.length)
      resultIndex += bytes.length
    }
    result
  }
}

object BigEndianToBytes extends ToBytes {
  def apply(i: Int): Array[Byte] = Array((i >>> 24).toByte, (i >>> 16).toByte, (i >>> 8).toByte, i.toByte)
  def apply(i: Short): Array[Byte] = Array((i >>> 8).toByte, i.toByte)
  def apply(i: Long): Array[Byte] =
    Array( 
      (i >>> 56).toByte,
      (i >>> 48).toByte,
      (i >>> 40).toByte,
      (i >>> 32).toByte,
      (i >>> 24).toByte,
      (i >>> 16).toByte,
      (i >>> 8).toByte,
      i.toByte
    )

  def apply(f: Float): Array[Byte] = apply(java.lang.Float.floatToIntBits(f))
  def apply(d: Double): Array[Byte] = apply(java.lang.Double.doubleToLongBits(d))
}

object LittleEndianToBytes extends ToBytes {
  def apply(i: Short): Array[Byte] = Array(i.toByte, (i >>> 8).toByte)
  def apply(i: Int): Array[Byte] = Array(i.toByte, (i >>> 8).toByte, (i >>> 16).toByte, (i >>> 24).toByte)
  def apply(i: Long): Array[Byte] =
    Array(
      i.toByte,
      (i >>> 8).toByte,
      (i >>> 16).toByte,
      (i >>> 24).toByte,
      (i >>> 32).toByte,
      (i >>> 40).toByte,
      (i >>> 48).toByte,
      (i >>> 56).toByte
    )

  def apply(f: Float): Array[Byte] = apply(java.lang.Float.floatToIntBits(f))
  def apply(d: Double): Array[Byte] = apply(java.lang.Double.doubleToLongBits(d))
}
