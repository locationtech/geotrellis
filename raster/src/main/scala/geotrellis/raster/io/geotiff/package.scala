package geotrellis.raster.io

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

package object geotiff {
  val DefaultCompression = DeflateCompression
  val Deflate = DeflateCompression

  implicit class GeoTiffTileMethods(val tile: Tile) extends AnyRef {
    def toGeoTiffTile(): GeoTiffTile =
      GeoTiffTile(tile)

    def toGeoTiffTile(options: GeoTiffOptions): GeoTiffTile =
      GeoTiffTile(tile, options)

    def toGeoTiffTile(compression: Compression): GeoTiffTile =
      GeoTiffTile(tile, GeoTiffOptions(compression))

    def toGeoTiffTile(layout: StorageMethod): GeoTiffTile =
      GeoTiffTile(tile, GeoTiffOptions(layout))
  }
}
