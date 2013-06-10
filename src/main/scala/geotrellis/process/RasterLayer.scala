package geotrellis.process

import scala.io.Source
import java.io.File
import geotrellis._
import geotrellis.util._
import geotrellis.data.FileReader
import geotrellis.data.arg.ArgReader

/**
 * Represents a Raster Layer that can give detailed information
 * about the Raster it represents, cache the raster, and get the 
 * raster cropped to an extent or at a different resolution.
 * 
 * This represents a layer in a bound Context, not an abstract
 * representation of the Raster. In other words, if you are
 * holding one of these objects, then the code that uses it
 * should only execute on the machine that the RasterLayer is
 * from. If you pass around RasterLayers, you will be passing around
 * the cache as well, which is not ideal.
 * 
 * To implement a new RasterLayer, inherit from this class, implement
 * the cache(c:Cache) method for caching the raster layer, and implement
 * the getRaster() (for getting a Raster with it's native RasterExtent) and
 * getRaster(rasterExtent:RasterExtent) (for getting a Raster at a different
 * extent\resolution). Optionally you can override getRaster(extent:Extent),
 * which by default just creates a RasterExtent with that extent snapped to 
 * the raster's native resolution.
 */
abstract class RasterLayer(val info:RasterLayerInfo) {
  private var _cache:Option[Cache] = None
  protected def getCache = 
    _cache match {
      case Some(c) => c
      case None =>
        sys.error("No cache is currently set. Check isCached before accessing this member.")
    }

  def setCache(c:Option[Cache]) = {
    _cache = c
  }

  private var _isCached = false
  def isCached = _isCached

  def cache():Unit = {
    _cache match {
      case Some(c) => 
        cache(c)
        _isCached = true
        info.cached = true
      case None => //pass
    }
  }

  protected def cache(c:Cache):Unit

  def getRaster():Raster = getRaster(None)
  def getRaster(targetExtent:Option[RasterExtent]):Raster

  def getRaster(extent:Extent):Raster = 
    getRaster(Some(info.rasterExtent.createAligned(extent)))
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
