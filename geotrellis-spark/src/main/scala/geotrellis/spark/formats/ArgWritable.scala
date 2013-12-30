package geotrellis.spark.formats

import org.apache.hadoop.io.BytesWritable
import geotrellis.RasterType
import geotrellis.raster.RasterData


class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes) {
  // This constructor is rarely used (e.g., hadoop fs -text). Preferred use is via companion object  
  def this() = this(Array.fill[Byte](0)(0))
}

object ArgWritable {
  def apply(len: Int, fillValue: Byte) = new ArgWritable(Array.fill[Byte](len)(fillValue))
  def apply(bytes: Array[Byte]) = new ArgWritable(bytes)
  def apply(aw: ArgWritable) = new ArgWritable(aw.getBytes)

  def toWritable(data: RasterData) = ArgWritable(data.toArrayByte)
  def toRasterData(aw: ArgWritable, awType: RasterType, cols: Int, rows: Int) = RasterData.fromArrayByte(aw.getBytes, awType, cols, rows)
}