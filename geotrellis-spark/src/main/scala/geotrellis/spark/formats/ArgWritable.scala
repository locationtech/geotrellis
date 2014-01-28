package geotrellis.spark.formats

import geotrellis._
import geotrellis.RasterType
import geotrellis.raster.RasterData

import org.apache.hadoop.io.BytesWritable

class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes) {
  // This constructor is rarely used (e.g., hadoop fs -text). Preferred use is via companion object  
  def this() = this(Array[Byte]())
  
  def toRasterData(awType: RasterType, cols: Int, rows: Int) = RasterData.fromArrayByte(getBytes, awType, cols, rows)
}

object ArgWritable {
  def apply(len: Int, fillValue: Byte) = new ArgWritable(Array.ofDim[Byte](len).fill(fillValue))
  def apply(bytes: Array[Byte]) = new ArgWritable(bytes)
  def apply(aw: ArgWritable) = new ArgWritable(aw.getBytes)

  def fromRasterData(data: RasterData) = ArgWritable(data.toArrayByte)
}