package geotrellis.spark.io.cog

import geotrellis.raster.{CellGrid, GridBounds}
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

import spray.json.JsonFormat

import java.net.URI

trait TiffMethods[V <: CellGrid] {
  def getGeoTiffInfo(uri: URI): GeoTiffReader.GeoTiffInfo

  def tileTiff[K](tiff: GeoTiff[V], gridBounds: Map[GridBounds, K]): Vector[(K, V)]
  def readTiff(uri: URI, index: Int): GeoTiff[V]
  def readTiff(bytes: Array[Byte], index: Int): GeoTiff[V]

  def cropTiff(tiff: GeoTiff[V], gridBounds: GridBounds): V
  def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds
  def getSegmentGridBounds(bytes: Array[Byte], index: Int): (Int, Int) => GridBounds
  def getKey[K: JsonFormat](uri: URI): K
}
