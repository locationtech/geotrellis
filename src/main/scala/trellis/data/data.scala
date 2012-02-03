package trellis.data

import scala.math.{Numeric, min, max, abs, round, floor, ceil}
import java.io.{File, FileInputStream, FileOutputStream}
import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import trellis._
import trellis.process._

trait ReadState {
  val layer:RasterLayer
  val target:RasterExtent

  // don't override
  def loadRaster(): IntRaster = {
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
    val ybase = target.extent.ymin - src_ymin + (dst_cellheight / 2)

    // track height/width in map units
    val src_map_width  = src_xmax - src_xmin
    val src_map_height = src_ymax - src_ymin

    // initialize the whole raster
    // TODO: only initialize the part we will read from
    val src_size = src_rows * src_cols
    initSource(0, src_size)
    
    // this is the resampled destination array
    val dst_size = dst_cols * dst_rows
    val resampled = Array.fill[Int](dst_size)(NODATA)

    // these are the min and max columns we will access on this row
    val min_col = (xbase / src_cellwidth).asInstanceOf[Int]
    val max_col = ((xbase + dst_cols * dst_cellwidth) / src_cellwidth).asInstanceOf[Int]

    // start at the Y-center of the first dst grid cell
    var y = ybase

    // loop over rows
    var dst_row = 0
    while (dst_row < dst_rows) {

      // calculate the Y grid coordinate to read from
      val src_row = (src_rows - (y / src_cellheight)).asInstanceOf[Int]
      //assert(src_row < src_rows)

      // pre-calculate some spans we'll use a bunch
      val src_span = src_row * src_cols
      val dst_span = (dst_rows - 1 - dst_row) * dst_cols

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

      // increase our X map coordinate
      y += dst_cellwidth
      dst_row += 1
    }

    // build a raster object from our array and return
    createRaster(resampled)
  }

  // don't usually override
  protected[this] def createRaster(data:Array[Int]) = {
    IntRaster(translate(data), target, layer.name)
  }

  // must override
  def getNoDataValue:Int

  // must override
  protected[this] def initSource(position:Int, size:Int):Unit

  // must override
  protected[this] def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int):Unit

  // maybe override
  def destroy() {}

  // maybe need to override
  protected[this] def translate(data:Array[Int]) = {
    var i = 0
    val nd = getNoDataValue
    val len = data.length
    while (i < len) {
      if (data(i) == nd) data(i) = NODATA
      i += 1
    }
    data
  }
}

trait Reader {}

trait FileReader extends Reader {
  def createReadState(path:String, layer:RasterLayer, target:RasterExtent):ReadState

  def metadataPath(path:String) = {
    val i = path.lastIndexOf(".")
    path.substring(0, i) + ".json"
  }

  def readMetadata(path:String) = RasterLayer.fromPath(metadataPath(path))

  def read(path:String, layerOpt:Option[RasterLayer], targetOpt:Option[RasterExtent]): IntRaster = {
    val layer = layerOpt.getOrElse(readMetadata(path))
    val target = targetOpt.getOrElse(layer.rasterExtent)

    val readState = createReadState(path, layer, target)
    val raster = readState.loadRaster() // all the works is here
    readState.destroy()

    raster
  }
}

trait Writer {
  def write(path:String, raster:IntRaster) { write(path, raster, raster.name) }

  def write(path:String, raster:IntRaster, name:String):Unit

  def writeMetadataJSON(path:String, name:String, re:RasterExtent) {
    val metadata = """{
  "layer": "%s",
  "xmin": %f,
  "xmax": %f,
  "ymin": %f,
  "ymax": %f,
  "cols": %d,
  "rows": %d,
  "cellwidth": %f,
  "cellheight": %f 
}""".format(name, re.extent.xmin, re.extent.xmax, re.extent.ymin,
             re.extent.ymax, re.cols, re.rows, re.cellwidth, re.cellheight)

    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(metadata.getBytes)
    bos.close
  }
}
