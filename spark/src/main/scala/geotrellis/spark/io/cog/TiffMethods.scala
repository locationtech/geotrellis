package geotrellis.spark.io.cog

import geotrellis.raster.{CellGrid, GridBounds}
import geotrellis.raster.io.geotiff.GeoTiff

import java.net.URI

trait TiffMethods[V <: CellGrid] {
  def readTiff(uri: URI, index: Int): GeoTiff[V]
  def tileTiff[K](tiff: GeoTiff[V], gridBounds: Map[GridBounds, K]): Vector[(K, V)]
  def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds
}
