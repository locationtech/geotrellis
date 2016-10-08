package geotrellis.spark.streaming.buffer

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._
import geotrellis.spark._
import geotrellis.spark.buffer.{BufferSizes, BufferTiles, BufferedTile}
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

class BufferTilesMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
](val self: DStream[(K, V)]) extends MethodExtensions[DStream[(K, V)]] {
  def bufferTiles(bufferSize: Int): DStream[(K, BufferedTile[V])] =
    self.transform(BufferTiles(_, bufferSize))

  def bufferTiles(bufferSize: Int, layerBounds: GridBounds): DStream[(K, BufferedTile[V])] =
    self.transform(BufferTiles(_, bufferSize, layerBounds))

  def bufferTiles(bufferSizesPerKey: DStream[(K, BufferSizes)]): DStream[(K, BufferedTile[V])] =
    self.transformWith(bufferSizesPerKey, (selfRdd: RDD[(K, V)], bufferSizesPerKeyRdd: RDD[(K, BufferSizes)]) => BufferTiles(selfRdd, bufferSizesPerKeyRdd))

  def bufferTiles(getBufferSizes: K => BufferSizes): DStream[(K, BufferedTile[V])] =
    self.transform(BufferTiles(_, getBufferSizes))
}

