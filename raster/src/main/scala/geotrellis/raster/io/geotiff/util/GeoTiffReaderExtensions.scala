package geotrellis.raster.io.geotiff.util

import geotrellis.util._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._

object GeoTiffReaderExtensions {
  trait Reader[T] {

    def readTags(bytesStreamer: BytesStreamer): TiffTags =
      readTags(StreamByteReader(bytesStreamer))

    def readTags(byteReader: StreamByteReader): TiffTags =
      TiffTagsReader.read(byteReader)

    def read(bytesStreamer: BytesStreamer): T =
      read(StreamByteReader(bytesStreamer))
    
    def read(byteReader: StreamByteReader): T
  }

  object Reader {

    implicit object TileReader extends Reader[Tile] {
      def read(byteReader: StreamByteReader): Tile =
        GeoTiffReader.readSingleband(byteReader)
    }

    /*
    implicit object MultibandTileReader extends Reader[MultibandTile] {
      def read(byteReader: StreamByteReader): MultibandTile =
        GeoTiffReader.readMultiband(byteReader)
    }
    */
  }
}
