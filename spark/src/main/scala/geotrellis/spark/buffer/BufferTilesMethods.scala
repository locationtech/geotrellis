package geotrellis.spark.buffer

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._
import geotrellis.spark._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class BufferTilesMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
](val self: RDD[(K, V)]) extends MethodExtensions[RDD[(K, V)]] {
  def bufferTiles(bufferSize: Int): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSize)

  def bufferTiles(bufferSize: Int, layerBounds: GridBounds): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSize, layerBounds)

  def bufferTiles(bufferSizesPerKey: RDD[(K, BufferSizes)]): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSizesPerKey)

  def bufferTiles(getBufferSizes: K => BufferSizes): RDD[(K, BufferedTile[V])] =
    BufferTiles(self, getBufferSizes)
}
