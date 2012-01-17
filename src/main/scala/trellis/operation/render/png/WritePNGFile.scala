package trellis.operation.render.png

import trellis.data._
import trellis.operation._
import trellis.process._
import trellis._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
case class WritePNGFile(r:Op[IntRaster], path:String,
                        colorBreaks:Array[(Int, Int)], noDataColor:Int,
                        transparent:Boolean)
extends SimpleOp[Unit] with PNGBase {
  def _value(context:Context) = {
    val raster = context.run(r)
    val writer = new PNGRenderer(raster, path, applyColorMap, noDataColor,
                                  transparent)
    writer.write
  }
}

case class WritePNGFile2(r:Op[IntRaster], path:String, noDataColor:Int, transparent:Boolean)
extends SimpleOp[Unit] {
  def _value(context:Context) = {
    val raster = context.run(r)
    val writer = new PNGWriterRGB2(raster, path, noDataColor, transparent)
    writer.write
  }
}
