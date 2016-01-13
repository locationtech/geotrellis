package geotrellis.gdal

import geotrellis.raster._
import geotrellis.vector.Extent

import org.gdal.gdal.Band
import org.gdal.gdal.gdal

import scala.collection.JavaConversions._

class RasterBand(band: Band, cols: Int, rows: Int) {
  lazy val bandNumber: Int = band.GetBand

  lazy val mask: RasterBand = {
    val m = band.GetMaskBand
    new RasterBand(m, m.getXSize, m.getYSize)
  }

  lazy val noDataValue: Option[Double] = {
    val arr = Array.ofDim[java.lang.Double](1)
    band.GetNoDataValue(arr)
    if(arr(0) != null) {
      Some(arr(0))
    } else {
      None
    }
  }

  lazy val rasterType: GdalDataType =
    band.getDataType()

  lazy val blockWidth: Int =
    band.GetBlockXSize

  lazy val blockHeight: Int =
    band.GetBlockYSize

  /** How should this band be interpreted as color? */
  lazy val colorCode: Int =
    band.GetRasterColorInterpretation

  lazy val colorName: String =
    gdal.GetColorInterpretationName(colorCode)

  lazy val colorTable: Option[(Vector[RasterColor], String)] = {
    val ct = band.GetRasterColorTable
    if(ct == null) None
    else Some(((0 until ct.GetCount)
      .map { i => new RasterColor(ct.GetColorEntry(i)) }
      .toVector
    ), gdal.GetPaletteInterpretationName(ct.GetPaletteInterpretation))
  }

  lazy val description: Option[String] = {
    val desc = band.GetDescription
    if(desc == null || desc.isEmpty) None
    else Some(desc)
  }

  lazy val categories: Seq[String] = {
    band.GetRasterCategoryNames.map(_.asInstanceOf[String]).toSeq
  }

  def metadata: List[String] =
    band.GetMetadata_List("").toList.map(_.asInstanceOf[String])

  def metadata(id: String): List[String] =
    band.GetMetadata_List(id).toList.map(_.asInstanceOf[String])

  lazy val checksum: Int = band.Checksum
  def checksum(colOffset: Int, rowOffset: Int, width: Int, height: Int): Int =
    band.Checksum(colOffset, rowOffset, width, height)

  // Stats
  lazy val (xmin, xmax) = {
    val arr = Array.ofDim[Double](2)
    band.ComputeRasterMinMax(arr)
    (arr(0),arr(1))
  }

  lazy val (mean, std) = {
    val arr = Array.ofDim[Double](2)
    band.ComputeBandStats(arr)
    (arr(0), arr(1))
  }

  /** This call will recover memory used to cache data blocks for this raster band, and ensure that new requests are referred to the underlying driver. */
  def flushCache(): Unit =
    band.FlushCache

  def dataShort(): Array[Short] = {
    val arr = Array.ofDim[Short](cols*rows)
    band.ReadRaster(0,0,cols,rows,IntConstantNoDataCellType16,arr)
    arr
  }

  def dataInt(): Array[Int] = {
    val arr = Array.ofDim[Int](cols*rows)
    band.ReadRaster(0,0,cols,rows,IntConstantNoDataCellType32,arr)
    arr
  }

  def dataFloat(): Array[Float] = {
    val arr = Array.ofDim[Float](cols*rows)
    band.ReadRaster(0,0,cols,rows,FloatConstantNoDataCellType32,arr)
    arr
  }

  def dataDouble(): Array[Double] = {
    val arr = Array.ofDim[Double](cols*rows)
    band.ReadRaster(0,0,cols,rows,FloatConstantNoDataCellType64,arr)
    arr
  }

  def toTile(): Tile = {
    val cellType = rasterType match {
      case TypeUnknown => geotrellis.raster.DoubleConstantNoDataCellType
      case ByteConstantNoDataCellType => geotrellis.raster.ShortConstantNoDataCellType // accounts for unsigned
      case TypeUInt16 => geotrellis.raster.IntConstantNoDataCellType // accounts for unsigned
      case IntConstantNoDataCellType16 => geotrellis.raster.ShortConstantNoDataCellType
      case TypeUInt32 => geotrellis.raster.FloatConstantNoDataCellType // accounts for unsigned
      case IntConstantNoDataCellType32 => geotrellis.raster.IntConstantNoDataCellType
      case FloatConstantNoDataCellType32 => geotrellis.raster.FloatConstantNoDataCellType
      case FloatConstantNoDataCellType64 => geotrellis.raster.DoubleConstantNoDataCellType
      case TypeCInt16 => ???
      case TypeCInt32 => ???
      case TypeCFloat32 => ???
      case TypeCFloat64 => ???
    }

    val tile = 
      (cellType match {
        case geotrellis.raster.ShortConstantNoDataCellType =>
          ShortArrayTile(dataShort, cols, rows)
        case geotrellis.raster.IntConstantNoDataCellType => 
          IntArrayTile(dataInt, cols, rows)
        case geotrellis.raster.FloatConstantNoDataCellType =>
          FloatArrayTile(dataFloat, cols, rows)
        case geotrellis.raster.DoubleConstantNoDataCellType =>
          DoubleArrayTile(dataDouble, cols, rows)
      }).mutable

    // Replace NODATA values
    noDataValue match {
      case Some(nd) =>
        var col = 0
        while(col < cols) {
          var row = 0
          while(row < rows) {
            if(tile.getDouble(col,row) == nd) { tile.set(col, row, NODATA) }
            row += 1
          }
          col += 1
        }
      case None =>
    }

    tile
  }
}
