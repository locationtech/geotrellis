package geotrellis.process.json

import geotrellis._
import geotrellis.process._

import com.typesafe.config.ConfigFactory

object RasterLayerParser {
  def parseType(s:String):RasterType = s match {
    case "bool" => TypeBit
    case "int8" => TypeByte
    case "int16" => TypeShort
    case "int32" => TypeInt
    case "float32" => TypeFloat
    case "float64" => TypeDouble
    case s => sys.error("unsupported datatype '%s'" format s)
  }

  def apply(jsonString:String,path:String = "") = {
        val json = ConfigFactory.parseString(jsonString)
        val layerType = json.getString("type").toLowerCase

        Catalog.getRasterLayerBuilder(layerType) match {
          case Some(builder) => builder(path,json)
          case None => 
            throw new java.io.IOException(s"Raster layer defined at $path has raster layer type $layerType " +
                                           "for which this catalog has no builder.")
        }
  }
}
