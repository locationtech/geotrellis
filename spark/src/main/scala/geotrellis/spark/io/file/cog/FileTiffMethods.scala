package geotrellis.spark.io.file.cog

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.{GridBounds, MultibandTile, Tile}
import geotrellis.spark.io.cog.TiffMethods
import geotrellis.util.Filesystem

import java.net.URI

trait FileTiffMethods {
  implicit val tiffMethods = new TiffMethods[Tile] {

    def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
      val tiff = GeoTiffReader.readSingleband(uri.getPath, false, true)
      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: GridBounds): Tile = {
      tiff.tile match {
        case gtTile: GeoTiffTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }
    }

    def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
      val info =
        readGeoTiffInfo(
          byteReader         = Filesystem.toMappedByteBuffer(uri.getPath),
          decompress         = false,
          streaming          = true,
          withOverviews      = true,
          byteReaderExternal = None
        )

      val geoTiffTile =
        GeoTiffReader.geoTiffSinglebandTile(info)

      val tiff =
        if(index < 0) geoTiffTile
        else geoTiffTile.overviews(index)

      val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

      func
    }
  }

  implicit val multibandTiffMethods = new TiffMethods[MultibandTile] {
    def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
      val tiff = GeoTiffReader.readMultiband(uri.getPath, false, true)

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def tileTiff[K](tiff: GeoTiff[MultibandTile], gridBounds: GridBounds): MultibandTile = {
      tiff.tile match {
        case gtTile: GeoTiffMultibandTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }
    }

    def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
      val info =
        readGeoTiffInfo(
          byteReader         = Filesystem.toMappedByteBuffer(uri.getPath),
          decompress         = false,
          streaming          = true,
          withOverviews      = true,
          byteReaderExternal = None
        )

      val geoTiffTile =
        GeoTiffReader.geoTiffMultibandTile(info)

      val tiff =
        if(index < 0) geoTiffTile
        else geoTiffTile.overviews(index)

      val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

      func
    }
  }
}
