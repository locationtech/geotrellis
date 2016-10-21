package geotrellis.raster.io.geotiff.util

import geotrellis.util.ByteReader
import geotrellis.raster.io.geotiff.tags._

import java.nio.Buffer

object GeoTiffType {
  trait OffsetType[T] {
    def position(byteReader: ByteReader, newPoint: T): Buffer
    def getTiffTagMetadata(byteReader: ByteReader): TiffTagMetadata
  }

  object OffsetType {

    implicit object OffsetTypeInt extends OffsetType[Int] {
      def position(byteReader: ByteReader, newPoint: Int): Buffer =
        byteReader.position(newPoint)

      def getTiffTagMetadata(byteReader: ByteReader): TiffTagMetadata =
        TiffTagMetadata(
          byteReader.getUnsignedShort,
          byteReader.getUnsignedShort,
          byteReader.getInt,
          byteReader.getInt
        )
    }
  }
}
