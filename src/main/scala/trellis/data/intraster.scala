package geotrellis.data

import scala.math.{Numeric,min,max,round,abs}
import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.channels.FileChannel.MapMode._

import geotrellis._
import geotrellis.process._

final class IntRasterReadState(val raster:IntRaster,
                               val target:RasterExtent) extends ReadState {
  val layer = RasterLayer(raster.name, raster.rasterExtent)

  var src:IntBuffer = null

  def getNoDataValue = NODATA

  def initSource(position:Int, size:Int) {
    src = IntBuffer.wrap(raster.data.asArray, position, size)
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = this.src.get(sourceIndex)
  }

  override def translate(data:Array[Int]) = data.clone()
}

object IntRasterReader extends Reader {
  def read(raster:IntRaster, targetOpt:Option[RasterExtent]): IntRaster = {
    val target = targetOpt.getOrElse(raster.rasterExtent)

    val readState = new IntRasterReadState(raster, target)
    val raster2 = readState.loadRaster()
    readState.destroy()

    raster2
  }
}

///**
//  * RasterReader for IntRasters already in memory.
//  * Useful when you want to use the RasterReader interface but also to accept Rasters already in memory. 
//  */
//class IntRasterReader(val raster:IntRaster) extends RasterReader {
//  var src:IntBuffer = null
//
//  val nodataval = NODATA
//
//  def initSource(position:Int, size:Int) {
//    this.src = IntBuffer.wrap(raster.data.asArray, position, size)
//  }
//
//  @inline
//  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
//    dest(destIndex) = this.src.get(sourceIndex)
//  }
//
//  def makeRaster(data:RasterData, geo:RasterExtent) = {
//    IntRaster(data, geo.rows, geo.cols, geo)
//  }
//
//  override def makeRaster(data:Array[Int], geo:RasterExtent) = {
//    IntRaster(data, geo.rows, geo.cols, geo)
//  }
//
//  def readMetadata {
//    this.xmin = raster.rasterExtent.extent.xmin
//    this.xmax = raster.rasterExtent.extent.xmax
//    this.ymin = raster.rasterExtent.extent.ymin
//    this.ymax = raster.rasterExtent.extent.ymax
//    this.cols = raster.cols
//    this.rows = raster.rows
//    this.cellwidth = raster.rasterExtent.cellwidth
//    this.cellheight = raster.rasterExtent.cellheight
//  }
//
//  def getRaster = {
//    this.readMetadata
//    val extent = Extent(xmin, ymin, xmax, ymax)
//    val rasterExtent = RasterExtent(extent, cellwidth, cellheight, cols, rows)
//    //TODO: copy data?
//    this.makeRaster(raster.data, rasterExtent)
//  }
//}
