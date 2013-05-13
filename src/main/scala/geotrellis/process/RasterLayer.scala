package geotrellis.process

import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.util._

object RasterLayer {
  /**
   * Build a RasterLayer instance given a path to a JSON file.
   */
  def fromPath(path:String) = {
    val base = Filesystem.basename(path)
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data, base)
  }

  def fromFile(f:File) = RasterLayer.fromPath(f.getAbsolutePath)

  /**
   * Build a RasterLayer instance given a JSON string.
   */
  def fromJSON(data:String, basePath:String) = {
    json.RasterLayerParser(data, basePath)
  }
}

case class RasterLayer(name:String, typ:String, datatyp:RasterType,
                       basePath:String, rasterExtent:RasterExtent,
                       epsg:Int, xskew:Double, yskew:Double) {
  def jsonPath = basePath + ".json"
  def rasterPath = basePath + "." + typ
}

