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

  def apply(jsonString:String,path:String = "", cache:Option[Cache] = None) = {
//    val rasterLayer =
//      try {
        val json = ConfigFactory.parseString(jsonString)
        val layerType = json.getString("type").toLowerCase

        Catalog.getRasterLayerBuilder(layerType) match {
          case Some(builder) => builder(path,json,cache)
          case None => 
            System.err.println(s"[ERROR]  Raster layer defined at $path has raster layer type $layerType " +
                                "for which this catalog has no builder.")
            System.err.println("          Skipping...")
            None
        }

  //       val layer = json.getString("layer")
  //       val ltype:RasterLayerType = json.getString("type").toLowerCase

  //       val datatype = parseType(json.getString("datatype"))

  //       val xmin = json.getDouble("xmin")
  //       val ymin = json.getDouble("ymin")
  //       val xmax = json.getDouble("xmax")
  //       val ymax = json.getDouble("ymax")
  //       val extent = Extent(xmin, ymin, xmax, ymax)

  //       val cellWidth = json.getDouble("cellwidth")
  //       val cellHeight = json.getDouble("cellheight")

  //       val epsg = json.getInt("epsg")
  //       val xskew = json.getDouble("xskew")
  //       val yskew = json.getDouble("yskew")

  //       ltype match {
  //         case ConstantRaster =>
  //           val cols = json.getInt("cols")
  //           val rows = json.getInt("rows")

  //           val rasterExtent = RasterExtent(extent, cellWidth, cellHeight, cols, rows)
  //           val info = RasterLayerInfo(layer, datatype, rasterExtent, epsg, xskew, yskew)

  //           if(datatype.isDouble) {

  //           } else {

  //           }

  //         case ArgFile =>
  //           val cols = json.getInt("cols")
  //           val rows = json.getInt("rows")

  //           val rasterExtent = RasterExtent(extent, cellWidth, cellHeight, cols, rows)
  //           val info = RasterLayerInfo(layer, datatype, rasterExtent, epsg, xskew, yskew)
  //           Some(new ArgFileRasterLayer(info,path,cache))
  //         case Tiled =>
  //           val layoutCols = json.getInt("layout_cols")
  //           val layoutRows = json.getInt("layout_rows")
  //           val pixelCols = json.getInt("pixel_cols")
  //           val pixelRows = json.getInt("pixel_rows")
  //           val cols = layoutCols * pixelCols
  //           val rows = layoutRows * pixelRows

  //           val rasterExtent = RasterExtent(extent, cellWidth, cellHeight, cols, rows)
  //           val info = RasterLayerInfo(layer, datatype, rasterExtent, epsg, xskew, yskew)
  //           Some(new ArgFileRasterLayer(info,path,cache))

  // //        case AsciiFile =>

  //       }
      // } catch {
      //   case e:Exception =>
      //     if(path != null && !path.isEmpty) {
      //       System.err.println(s"Could not read raster layer at path $path: ${e.getMessage}")
      //     } else {
      //       System.err.println(s"Could not read raster layer: ${e.getMessage}")
      //     }
      //     None
      // }
//    rasterLayer
  }
}
