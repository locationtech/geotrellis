package geotrellis.spark.formats

import geotrellis._
import geotrellis.RasterType
import geotrellis.raster.RasterData

import org.apache.hadoop.io.BytesWritable

class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes) {
  // This constructor is rarely used (e.g., hadoop fs -text). Preferred use is via companion object  
  def this() = this(Array[Byte]())
}

object ArgWritable {
  def apply(len: Int, fillValue: Byte) = new ArgWritable(Array.ofDim[Byte](len).fill(fillValue))
  def apply(bytes: Array[Byte]) = new ArgWritable(bytes)
  def apply(aw: ArgWritable) = new ArgWritable(aw.getBytes)

  def toWritable(data: RasterData) = ArgWritable(data.toArrayByte)
  def toRasterData(aw: ArgWritable, awType: RasterType, cols: Int, rows: Int) = RasterData.fromArrayByte(aw.getBytes, awType, cols, rows)
}