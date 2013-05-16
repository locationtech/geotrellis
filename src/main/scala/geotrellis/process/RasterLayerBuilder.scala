package geotrellis.process

import geotrellis._

import com.typesafe.config.Config

/**
 * Defines a RasterLayerBuilder that can be used to add raster layer types
 * to GeoTrellis. Also provides some baseline helper functions for getting
 * Information out of the metadata json files.
 */
trait RasterLayerBuilder {
  def apply(path:String, json:Config, cache:Option[Cache]):Option[RasterLayer]

  def getName(json:Config) = json.getString("layer")

  def getExtent(json:Config) = {
    val xmin = json.getDouble("xmin")
    val ymin = json.getDouble("ymin")
    val xmax = json.getDouble("xmax")
    val ymax = json.getDouble("ymax")
    Extent(xmin, ymin, xmax, ymax)
  }

  def getCellWidthAndHeight(json:Config):(Double,Double) = {
    val cellWidth = json.getDouble("cellwidth")
    val cellHeight = json.getDouble("cellheight")
    (cellWidth,cellHeight)
  }

  def getEpsg(json:Config) = json.getInt("epsg")
  def getXskew(json:Config) = json.getDouble("xskew")
  def getYskew(json:Config) = json.getDouble("yskew")

  def getRasterType(json:Config):RasterType = {
    parseType(json.getString("datatype"))
  }

  def parseType(s:String):RasterType = s match {
    case "bool" => TypeBit
    case "int8" => TypeByte
    case "int16" => TypeShort
    case "int32" => TypeInt
    case "float32" => TypeFloat
    case "float64" => TypeDouble
    case s => sys.error("unsupported datatype '%s'" format s)
  }
}
