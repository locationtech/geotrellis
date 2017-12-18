package geotrellis.spark.io.file.cog

import java.net.URI

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.{GridBounds, MultibandTile, Tile}
import geotrellis.spark.io.cog.TiffMethods

trait FileTiffMethods {
  implicit val tiffMethods = new TiffMethods[Tile] {

    def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
      val (reader, ovrReader) = FileCOGRDDReader.getReaders(uri)

      val tiff =
        GeoTiffReader
          .readSingleband(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = ovrReader
          )

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: GridBounds): Tile = {
      tiff.tile match {
        case gtTile: GeoTiffTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }
    }

    override def segments(tiff: GeoTiff[Tile]): List[(GridBounds, Tile)] =
      tiff.tile match {
        case gtTile: GeoTiffTile => {
          println(s"ohlul: ${(0 to gtTile.segmentCount).map { i =>
            i -> gtTile.segmentLayout.getGridBounds(i)
          }}")

          gtTile
            .crop((0 to gtTile.segmentCount).map(gtTile.segmentLayout.getGridBounds(_)))
            .collect { case (gb, tile) if !tile.isNoDataTile =>
              gb -> tile
            }
            .toList
        }
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }

    def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
      val (reader, ovrReader) = FileCOGRDDReader.getReaders(uri)

      val info =
        readGeoTiffInfo(
          byteReader         = reader,
          decompress         = false,
          streaming          = true,
          withOverviews      = true,
          byteReaderExternal = ovrReader
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
      val (reader, ovrReader) = FileCOGRDDReader.getReaders(uri)

      val tiff =
        GeoTiffReader
          .readMultiband(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = ovrReader
          )

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
      val (reader, ovrReader) = FileCOGRDDReader.getReaders(uri)

      val info =
        readGeoTiffInfo(
          byteReader         = reader,
          decompress         = false,
          streaming          = true,
          withOverviews      = true,
          byteReaderExternal = ovrReader
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
