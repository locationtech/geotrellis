package geotrellis.spark.buffer

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class CollectionBufferTilesMethods[
  K: SpatialComponent,
  V <: CellGrid: Stitcher: (? => CropMethods[V])
](val self: Seq[(K, V)]) extends MethodExtensions[Seq[(K, V)]] {
  def bufferTiles(bufferSize: Int): Seq[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSize)

  def bufferTiles(bufferSize: Int, layerBounds: GridBounds): Seq[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSize, layerBounds)

  def bufferTiles(bufferSizesPerKey: Seq[(K, BufferSizes)]): Seq[(K, BufferedTile[V])] =
    BufferTiles(self, bufferSizesPerKey)

  def bufferTiles(getBufferSizes: K => BufferSizes): Seq[(K, BufferedTile[V])] =
    BufferTiles(self, getBufferSizes)
}
