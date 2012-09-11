package geotrellis.data

import scala.math.{Numeric, min, max, abs, round, floor, ceil}
import java.io.{File, FileInputStream, FileOutputStream}
import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import geotrellis._
import geotrellis.process._
import geotrellis.raster.IntConstant

trait ReadState {
  val layer:RasterLayer
  val target:RasterExtent

  // must override
  def getType: RasterType

  // must override
  def createRasterData(cols:Int, rows:Int):MutableRasterData = RasterData.emptyByType(getType, cols, rows)

  // must override
  protected[this] def initSource(position:Int, size:Int):Unit

  // must override
  protected[this] def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int):Unit

  // don't usually override
  protected[this] def createRaster(data:MutableRasterData) = Raster(data, target)

  // maybe override
  def destroy() {}

  // maybe need to override; currently a noop
  protected[this] def translate(data:MutableRasterData): Unit = ()

  // don't override
  def loadRaster(): Raster = {
    val re = layer.rasterExtent

    // keep track of cell size in our source raster
    val src_cellwidth =  re.cellwidth
    val src_cellheight = re.cellheight
    val src_cols = re.cols
    val src_rows = re.rows
    val src_xmin = re.extent.xmin
    val src_ymin = re.extent.ymin
    val src_xmax = re.extent.xmax
    val src_ymax = re.extent.ymax

    // the dimensions to resample to
    val dst_cols = target.cols
    val dst_rows = target.rows

    // calculate the dst cell size
    val dst_cellwidth  = (target.extent.xmax - target.extent.xmin) / dst_cols
    val dst_cellheight = (target.extent.ymax - target.extent.ymin) / dst_rows

    // save "normalized map coordinates" for destination cell (0, 0)
    val xbase = target.extent.xmin - src_xmin + (dst_cellwidth / 2)
    val ybase = target.extent.ymax - src_ymin - (dst_cellheight / 2)

    // track height/width in map units
    val src_map_width  = src_xmax - src_xmin
    val src_map_height = src_ymax - src_ymin

    // initialize the whole raster
    // TODO: only initialize the part we will read from
    val src_size = src_rows * src_cols
    initSource(0, src_size)
    
    // this is the resampled destination array
    val dst_size = dst_cols * dst_rows
    val resampled = createRasterData(dst_cols, dst_rows)

    // these are the min and max columns we will access on this row
    val min_col = (xbase / src_cellwidth).asInstanceOf[Int]
    val max_col = ((xbase + dst_cols * dst_cellwidth) / src_cellwidth).asInstanceOf[Int]

    // start at the Y-center of the first dst grid cell
    var y = ybase

    // loop over rows
    var dst_row = 0
    while (dst_row < dst_rows) {

      // calculate the Y grid coordinate to read from
      val src_row = (src_rows - (y / src_cellheight).asInstanceOf[Int] - 1)
      //assert(src_row < src_rows)

      // pre-calculate some spans we'll use a bunch
      val src_span = src_row * src_cols
      val dst_span = dst_row * dst_cols

      // xyz
      if (src_span + min_col < src_size && src_span + max_col >= 0) {

        // start at the X-center of the first dst grid cell
        var x = xbase
  
        // loop over cols
        var dst_col = 0
        while (dst_col < dst_cols) {
  
          // calculate the X grid coordinate to read from
          val src_col = (x / src_cellwidth).asInstanceOf[Int]
          //assert(src_col < src_cols)
  
          // compute src and dst indices and ASSIGN!
          val src_i = src_span + src_col

          if (src_col >= 0 && src_col < src_cols && src_i < src_size && src_i >= 0) {

            val dst_i = dst_span + dst_col
            assignFromSource(src_i, resampled, dst_i)
          }
  
          // increase our X map coordinate
          x += dst_cellwidth
          dst_col += 1
        }
      }

      // decrease our Y map coordinate
      y -= dst_cellheight
      dst_row += 1
    }

    // build a raster object from our array and return
    translate(resampled)
    createRaster(resampled)
  }
}

trait IntReadState extends ReadState {
  // must override
  def getNoDataValue:Int

  protected[this] override def translate(data:MutableRasterData) {
    var i = 0
    val len = data.length
    val nd = getNoDataValue
    while (i < len) {
      if (data(i) == nd) data(i) = NODATA
      i += 1
    }
  }
}

trait Reader {}

trait FileReader extends Reader {
  def metadataPath(path:String) = path.substring(0, path.lastIndexOf(".")) + ".json"

  def readMetadata(path:String) = RasterLayer.fromPath(metadataPath(path))

  def readStateFromCache(bytes:Array[Byte], layer:RasterLayer, target:RasterExtent):ReadState

  def readCache(bytes:Array[Byte], layer:RasterLayer, targetOpt:Option[RasterExtent]): Raster = {
    val target = targetOpt.getOrElse(layer.rasterExtent)
    val readState = readStateFromCache(bytes, layer, target)
    val raster = readState.loadRaster() // all the work is here
    readState.destroy()
    raster
  }

  def readStateFromPath(path:String, layer:RasterLayer, target:RasterExtent):ReadState

  def readPath(path:String, layerOpt:Option[RasterLayer], targetOpt:Option[RasterExtent]): Raster = {
    val layer = layerOpt.getOrElse(readMetadata(path))
    val target = targetOpt.getOrElse(layer.rasterExtent)
    if (! (new File(path)).exists() ) {
      Raster(IntConstant(NODATA,target.cols,target.rows),target)
    } else {
      val readState = readStateFromPath(path, layer, target)
      val raster = readState.loadRaster() // all the work is here
      readState.destroy()
      raster
    }
  }
}

trait Writer {
  def write(path:String, raster:Raster, name:String):Unit

  def rasterType: String
  def dataType:String

  def writeMetadataJSON(path:String, name:String, re:RasterExtent) {
    val metadata = """{
  "layer": "%s",
  "datatype": "%s", 
  "type": "%s",
  "xmin": %f,
  "xmax": %f,
  "ymin": %f,
  "ymax": %f,
  "cols": %d,
  "rows": %d,
  "cellwidth": %f,
  "cellheight": %f,
  "epsg": 3785,
  "yskew": 0.0,
  "xskew": 0.0
}""".format(name, rasterType, dataType, re.extent.xmin, re.extent.xmax, re.extent.ymin,
             re.extent.ymax, re.cols, re.rows, re.cellwidth, re.cellheight)

    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(metadata.getBytes)
    bos.close
  }
}
