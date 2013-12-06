package geotrellis.spark.formats

import org.apache.hadoop.io.BytesWritable

import geotrellis.RasterType
import geotrellis.raster.RasterData

class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes)

object ArgWritable {

  def apply = new ArgWritable(Array.fill[Byte](1)(0))
  def apply(bytes: Array[Byte]) = new ArgWritable(bytes)
  def apply(aw: ArgWritable) = new ArgWritable(aw.getBytes)

  def toWritable(data: RasterData) = ArgWritable(data.toArrayByte)
  def toRasterData(aw: ArgWritable, awType: RasterType, cols: Int, rows: Int) = RasterData.toRasterData(aw.getBytes, awType, cols, rows)
}