package geotrellis.process.json

import geotrellis._
import geotrellis.process.RasterLayer

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

  def apply(jsonString:String,path:String = ""):RasterLayer = {
    val json = ConfigFactory.parseString(jsonString)

    val layer = json.getString("layer")

    val datatype = parseType(json.getString("datatype"))
    val ltype = json.getString("type").toLowerCase
    val isTiled = ltype == "tiled"

    val xmin = json.getDouble("xmin")
    val ymin = json.getDouble("ymin")
    val xmax = json.getDouble("xmax")
    val ymax = json.getDouble("ymax")
    val extent = Extent(xmin, ymin, xmax, ymax)

    val cellWidth = json.getDouble("cellwidth")
    val cellHeight = json.getDouble("cellheight")

    val rasterExtent = if(isTiled) {

      val layoutCols = json.getInt("layout_cols")
      val layoutRows = json.getInt("layout_rows")
      val pixelCols = json.getInt("pixel_cols")
      val pixelRows = json.getInt("pixel_rows")
      val cols = layoutCols * pixelCols
      val rows = layoutRows * pixelRows

      RasterExtent(extent, cellWidth, cellHeight, cols, rows)
    } else {
      val cols = json.getInt("cols")
      val rows = json.getInt("rows")

      RasterExtent(extent, cellWidth, cellHeight, cols, rows)
    }

    var epsg = json.getInt("epsg")
    var xskew = json.getDouble("xskew")
    var yskew = json.getDouble("yskew")

    RasterLayer(layer,ltype, datatype, path, rasterExtent, epsg, xskew, yskew)
  }
}
