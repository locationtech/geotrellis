package geotrellis.data

import java.nio.IntBuffer
import geotrellis._
import geotrellis.process._
import geotrellis.util._

final class RasterReadState(raster:Raster,
                            val target:RasterExtent) extends ReadState {
  val data:ArrayRasterData = raster.data.asInstanceOf[ArrayRasterData] //FIXME

  val layer = RasterLayer("raster", "intraster", "", "", raster.rasterExtent, 3857, 0.0, 0.0)
  //var src:IntBuffer = null

  //def getNoDataValue = NODATA

  //def createRasterData(size:Int) = IntArrayRasterData.empty(size)
  def getType = data.getType

  private var pos:Int = 0
  private var sz:Int = 0
  def initSource(position:Int, size:Int) {
    pos = position
    sz = size
    //src = IntBuffer.wrap(raster.toArray, position, size)
  }

  @inline def assignFromSource(sourceIndex:Int, dest:StrictRasterData, destIndex:Int) {
    //dest(destIndex) = src.get(sourceIndex)
    dest(destIndex) = data.apply(sourceIndex + pos)
  }
}

object RasterReader extends Reader {
  def read(raster:Raster, targetOpt:Option[RasterExtent]): Raster = {
    val target = targetOpt.getOrElse(raster.rasterExtent)

    val readState = new RasterReadState(raster, target)
    val raster2 = readState.loadRaster()
    readState.destroy()

    raster2
  }
}
