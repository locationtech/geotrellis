package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.util._
import geotrellis.data.arg.ArgReader

import java.io.{File, BufferedReader}
import com.typesafe.config.Config

object AsciiRasterLayerBuilder
extends RasterLayerBuilder {
  val intRe = """^(-?[0-9]+)$""".r
  val floatRe = """^(-?[0-9]+\.[0-9]+)$""".r

  def apply(jsonPath:String, json:Config):Option[RasterLayer] = {
    val path = 
      if(json.hasPath("path")) {
        json.getString("path")
      } else {
        val ascPath = Filesystem.basename(jsonPath) + ".asc"
        if(!new java.io.File(ascPath).exists) {
          ascPath
        } else {
          Filesystem.basename(jsonPath) + ".grd"
        }
      }

    val rasterType:RasterType = 
      if(json.hasPath("type")) {
        val t = getRasterType(json)
        if(t.isDouble) {
          System.err.println(s"[ERROR] Layer at $jsonPath has Ascii type and a Double data type. " +
                              "This is not currently supported.")
          System.err.println("[ERROR]   Skipping this raster layer.")
          return None
        }
        t
      } else {
        TypeInt
      }
    if(!new java.io.File(path).exists) {
      System.err.println("[ERROR] Cannot find data (.asc or .grd file) for " +
        s"Ascii Raster '${getName(json)}' in catalog.")
      System.err.println("[ERROR]   Skipping this raster layer.")
      None
    } else {
      val (rasterExtent,noDataValue) = loadMetaData(path)

      val info = RasterLayerInfo(getName(json),
        rasterType,
        rasterExtent,
        getEpsg(json),
        getXskew(json),
        getYskew(json))

      Some(new AsciiRasterLayer(info,noDataValue,path))
    }
  }

  def fromFile(path:String, cache:Option[Cache] = None):AsciiRasterLayer = {
    val f = new File(path)
    if(!f.exists) {
      sys.error(s"Path $path does not exist")
    }
    val (rasterExtent,noDataValue) = loadMetaData(path)

    val name = Filesystem.basename(f.getName)

    val info = RasterLayerInfo(name,
      TypeInt,
      rasterExtent,
      0,
      0,
      0)

    new AsciiRasterLayer(info,noDataValue,path)
  }

  def getBufferedReader(path:String) = {
    val fh = new File(path)
    if (!fh.canRead) throw new Exception("you can't read '%s' so how can i?".format(path))
    val fr = new java.io.FileReader(path)
    new BufferedReader(fr)
  }

  def loadMetaData(path:String):(RasterExtent,Int) = {
    var ncols:Int = 0
    var nrows:Int = 0
    var xllcorner:Double = 0.0
    var yllcorner:Double = 0.0
    var cellsize:Double = 0.0
    var nodata_value:Int = -9999

    val br = getBufferedReader(path)

    try {
      var done = false
      while (!done) {
        val line = br.readLine().trim()
        val toks = line.split(" ")
  
        if (line == null) throw new Exception("premature end of file: %s".format(path))
        if (toks.length == 0) throw new Exception("illegal empty line: %s".format(path))
  
        if (line.charAt(0).isDigit) {
          done = true
        } else {
          toks match {
            case Array("nrows", intRe(n)) => nrows = n.toInt
            case Array("ncols", intRe(n)) => ncols = n.toInt
            case Array("xllcorner", floatRe(n)) => xllcorner = n.toDouble
            case Array("yllcorner", floatRe(n)) => yllcorner = n.toDouble
            case Array("cellsize", floatRe(n)) => cellsize = n.toDouble
            case Array("nodata_value", intRe(n)) => nodata_value = n.toInt
  
            case _ => throw new Exception("mal-formed line '%s'".format(line))
          }
        }
      }
    } finally {
      br.close()
    }

    val xmin = xllcorner
    val ymin = yllcorner
    val xmax = xllcorner + ncols * cellsize
    val ymax = yllcorner + nrows * cellsize
    val e = Extent(xmin, ymin, xmax, ymax)

    (RasterExtent(e, cellsize, cellsize, ncols, nrows), nodata_value)
  }
}

class AsciiRasterLayer(info:RasterLayerInfo, noDataValue:Int, rasterPath:String) 
extends UntiledRasterLayer(info) {
  private var cached = false

  def getRaster(targetExtent:Option[RasterExtent]) =
    if(isCached) {
      getCache.lookup[Array[Byte]](info.name) match {
        case Some(bytes) =>
          getReader.readCache(bytes, info.rasterType, info.rasterExtent, targetExtent)
        case None =>
          sys.error("Cache problem: Layer things it's cached but it is in fact not cached.")
      }
    } else {
      getReader.readPath(info.rasterType,info.rasterExtent,targetExtent)
    }

  def cache(c:Cache) = 
    c.insert(info.name, Filesystem.slurp(rasterPath))

  private def getReader = new AsciiReader(rasterPath, noDataValue)
}
