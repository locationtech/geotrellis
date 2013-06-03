package geotrellis.process

import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.util._
import geotrellis.data.FileReader
import geotrellis.data.arg.ArgReader


abstract class RasterLayer(val info:RasterLayerInfo,
                           private val c:Option[Cache]) {
  def getRaster():Raster = getRaster(None)

  def getRaster(targetExtent:Option[RasterExtent]):Raster
  def cache():Unit
}

object RasterLayer {
  /**
   * Build a RasterLayer instance given a path to a JSON file.
   */
  def fromPath(path:String, cache:Option[Cache] = None) = {
    val base = Filesystem.basename(path) + ".json"
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data, base, cache)
  }

  def fromFile(f:File, cache:Option[Cache] = None) = RasterLayer.fromPath(f.getAbsolutePath, cache)

  /**
   * Build a RasterLayer instance given a JSON string.
   */
  def fromJSON(data:String, basePath:String, cache:Option[Cache] = None):Option[RasterLayer] = {
    json.RasterLayerParser(data, basePath, cache)
  }
}
