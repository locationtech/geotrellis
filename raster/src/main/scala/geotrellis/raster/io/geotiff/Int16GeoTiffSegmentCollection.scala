package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import spire.syntax.cfor._

trait Int16GeoTiffSegmentCollection extends GeoTiffSegmentCollection {
  type T = Int16GeoTiffSegment

  val bandType = Int16BandType
  val noDataValue: Option[Short]

  val createSegment: Int => Int16GeoTiffSegment = noDataValue match {
    case None =>
      { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i)) with Int16RawSegment }
    case Some(nd) if (nd == Short.MinValue) =>
      { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i)) with Int16ConstantNoDataSegment }
    case Some(nd) =>
      { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i)) with Int16UserDefinedNoDataSegment { val noDataValue = nd } }
  }
}
