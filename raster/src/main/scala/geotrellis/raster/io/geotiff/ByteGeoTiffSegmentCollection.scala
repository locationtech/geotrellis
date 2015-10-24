package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._

trait ByteGeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = ByteGeoTiffSegment

  val bandType = ByteBandType
  val cellType = TypeByte

  val noDataValue: Option[Double]

  val createSegment: Int => ByteGeoTiffSegment =
    noDataValue match {
      case Some(nd) if isData(nd) && Byte.MinValue.toDouble <= nd && nd <= Byte.MaxValue.toDouble =>        
        { i: Int => new NoDataByteGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
      case _ =>
        { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i)) }
    }
}
