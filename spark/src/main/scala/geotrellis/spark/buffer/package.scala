package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object buffer {
  implicit class BufferTilesMethodsWrapper[K: SpatialComponent: ClassTag, V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])](val self: RDD[(K, V)])
      extends MethodExtensions[RDD[(K, V)]] {
    def bufferTiles(borderSize: Int): RDD[(K, BufferedTile[V])] =
      BufferTiles(self, borderSize)

    def bufferTiles(borderSize: Int, layerBounds: GridBounds): RDD[(K, BufferedTile[V])] =
      BufferTiles(self, borderSize, layerBounds)

    def bufferTiles(borderSizesPerKey: RDD[(K, BorderSizes)]): RDD[(K, BufferedTile[V])] =
      BufferTiles(self, borderSizesPerKey)

    def bufferTiles(getBorderSizes: K => BorderSizes): RDD[(K, BufferedTile[V])] =
      BufferTiles(self, getBorderSizes)
  }
}
