package geotrellis.spark.io.hadoop.cog

import java.net.URI
import java.nio.ByteBuffer

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.{GridBounds, MultibandTile, Tile}
import geotrellis.spark.io.cog.TiffMethods
import geotrellis.util.ByteReader

trait HadoopTiffMethods {
  implicit val tiffMethods = new TiffMethods[Tile] {

    override def readTiff(bytes: Array[Byte], index: Int): GeoTiff[Tile] = {
      val tiff = GeoTiffReader.readSingleband(bytes)

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
      val (reader, ovrReader) = HadoopCOGRDDReader.getReaders(uri)

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

    override def getSegmentGridBounds(bytes: Array[Byte], index: Int): (Int, Int) => GridBounds = {
      val info =
        readGeoTiffInfo(
          byteReader         = ByteBuffer.wrap(bytes),
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

    def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
      val (reader, ovrReader) = HadoopCOGRDDReader.getReaders(uri)

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
    override def readTiff(bytes: Array[Byte], index: Int): GeoTiff[MultibandTile] = {
      val tiff = GeoTiffReader.readMultiband(bytes)

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
      val (reader, ovrReader) = HadoopCOGRDDReader.getReaders(uri)

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
      val (reader, ovrReader) = HadoopCOGRDDReader.getReaders(uri)

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
