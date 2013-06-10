package geotrellis.process

import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.util._
import geotrellis.data.FileReader
import geotrellis.data.arg.ArgReader


abstract class RasterLayer(val info:RasterLayerInfo) {
  protected var _cache:Option[Cache] = None
  def setCache(c:Option[Cache]) = {
    _cache = c
  }

  def getRaster():Raster = getRaster(None)
  def getRaster(targetExtent:Option[RasterExtent]):Raster

  def getRaster(extent:Extent):Raster = 
    getRaster(Some(info.rasterExtent.createAligned(extent)))

  def cache():Unit
}

object RasterLayer {
  /**
   * Build a RasterLayer instance given a path to a JSON file.
   */
  def fromPath(path:String) = {
    val base = Filesystem.basename(path) + ".json"
    val src = Source.fromFile(path)
    val data = src.mkString
    src.close()
    fromJSON(data, base)
  }

  def fromFile(f:File) = RasterLayer.fromPath(f.getAbsolutePath)

  /**
   * Build a RasterLayer instance given a JSON string.
   */
  def fromJSON(data:String, basePath:String):Option[RasterLayer] = {
    json.RasterLayerParser(data, basePath)
  }
}
