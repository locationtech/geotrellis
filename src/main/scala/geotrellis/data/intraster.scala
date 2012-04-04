package geotrellis.data

import java.nio.IntBuffer
import geotrellis._
import geotrellis.process._
import geotrellis.util._

final class IntRasterReadState(raster:IntRaster,
                               val target:RasterExtent) extends ReadState {
  val layer = RasterLayer(raster.name, "intraster", "", "", raster.rasterExtent, 3857, 0.0, 0.0)
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
