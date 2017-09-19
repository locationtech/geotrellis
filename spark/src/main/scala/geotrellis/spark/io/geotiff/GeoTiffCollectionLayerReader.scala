package geotrellis.spark.io.geotiff

import geotrellis.raster.{CellGrid, Raster}
import geotrellis.spark.LayerId
import geotrellis.vector.Extent

import java.net.URI

trait GeoTiffCollectionLayerReader[T <: CellGrid] {
  val seq: Seq[(Extent, URI)]
  val discriminator: URI => String

  def read(layerId: LayerId)(x: Int, y: Int): Raster[T]
  def readAll(layerId: LayerId): Seq[Raster[T]]
}
