package geotrellis.raster.io.geotiff

import geotrellis.raster._

trait UInt16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = UInt16GeoTiffSegment

  val bandType = UInt16BandType
  val noDataValue: Option[Int]

  val createSegment: Int => UInt16GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) with UInt16RawSegment }
    case Some(nd) if (nd == 0) =>
      { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) with UInt16ConstantNoDataSegment }
    case Some(nd) => // Cast nodata to int in this case so that we can properly compare it to the upcast unsigned byte
      { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) with UInt16UserDefinedNoDataSegment { val userDefinedIntNoDataValue = nd } }
    }
}
