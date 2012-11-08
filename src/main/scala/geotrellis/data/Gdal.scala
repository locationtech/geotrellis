package geotrellis.data

import scala.collection.immutable.HashMap
import scala.sys.process._

import geotrellis._
import geotrellis.raster._

/**
 * Utility for running GDAL commands.
 */
object Gdal {
  val gdalinfo = "gdalinfo"
  val gdal_translate = "gdal_translate"

  val gdalinfo_size_regex = """Size is (\d+), (\d+)""".r
  val gdalinfo_type_regex = """Type=(\w+)""".r
  val gdalinfo_pixelsize_regex = """Pixel Size = \((-?\d+.?\d*),(-?\d+.?\d*)\)""".r
  val gdalinfo_origin_regex = """Origin = \((-?\d+.?\d*),(-?\d+.?\d*)\)""".r

  /**
   * Converts a RasterType into the appropraite GDAL type description
   */
  def convertToGdalType: PartialFunction[RasterType,String] = {
    case TypeBit => "Int16"
    case TypeByte => "Int16"
    case TypeShort => "Int16"
    case TypeInt => "Int32"
    case TypeFloat => "Float32"
    case TypeDouble => "Float64"
  }

  /**
   * Converts the GDAL type description to a RasterType if it is supported, else None
   */
  def convertToRasterType: PartialFunction[String,Option[RasterType]] = {
    case "Byte" => Some(TypeByte)
    case "Int16" => Some(TypeShort)
    case "Int32" => Some(TypeInt)
    case "Float32" => Some(TypeFloat)
    case "Float64" => Some(TypeDouble)
    case _ => None
  }

  /**
   * Runs the gdalinfo command and gets the raster type and extent
   */
  def info(inPath: String): GdalRasterInfo = {
    val output = gdalinfo :: inPath :: Nil !!
    val (cols,rows) = gdalinfo_size_regex findFirstIn output match {
      case Some(gdalinfo_size_regex(w,h)) => (w.toInt,h.toInt)
      case _ => throw new RuntimeException("GDAL output did not include size information in file %s".format(inPath))
    }
    val rasterType = gdalinfo_type_regex findFirstIn output match {
      case Some(gdalinfo_type_regex(t)) => convertToRasterType(t)
      case _ => throw new RuntimeException("GDAL output did not include type information for file %s".format(inPath))
    }
    val (pixelX,pixelY) = gdalinfo_pixelsize_regex findFirstIn output match {
      case Some(gdalinfo_pixelsize_regex(w,h)) => (w.toDouble,h.toDouble)
      case _ => throw new RuntimeException("GDAL output did not include Pixel Size information for file %s".format(inPath))
    }
    val (originX,originY) = gdalinfo_origin_regex findFirstIn output match {
      case Some(gdalinfo_origin_regex(x,y)) => (x.toDouble,y.toDouble)
      case _ => throw new RuntimeException("GDAL output did not include Origin for file %s.".format(inPath))
    }

    val x2 = (cols * pixelX) + originX
    val y2 = (rows * pixelY) + originY

    val e = Extent(math.min(originX,x2), math.min(originY,y2), math.max(originX,x2), math.max(originY,y2))
    val rasterExtent = RasterExtent(e, math.abs(pixelX), math.abs(pixelY), cols, rows)

    GdalRasterInfo(rasterType, rasterExtent)
  }

  /**
   * Runs gdal_translate to convert a raster data file into ARG format.
   */
  def translate(inPath: String, outPath: String, rasterType: RasterType) = {
    gdal_translate :: "-of" :: " ARG" :: "-ot" :: convertToGdalType(rasterType) :: inPath :: outPath :: Nil !
  }

  /**
   * Runs gdal_translate to convert a subset of a raster data file into ARG format based off of
   * an offset and dimensions
   */
  def translate(inPath: String, outPath: String, rasterType: RasterType, offsetX: Int, offsetY: Int, width: Int, height: Int) = {
    val srcwin = "-srcwin" :: offsetX.toString :: offsetY.toString :: width.toString :: height.toString :: Nil
    val cmd = gdal_translate :: "-of" :: "ARG" :: "-ot" :: convertToGdalType(rasterType) :: srcwin ::: (inPath :: outPath :: Nil)
    cmd !
  }
}

case class GdalRasterInfo(rasterType: Option[RasterType], rasterExtent: RasterExtent)
