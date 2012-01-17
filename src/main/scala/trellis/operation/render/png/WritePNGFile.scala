package trellis.operation.render.png

import trellis._
import trellis.data._
import trellis.operation._
import trellis.process._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
case class WritePNGFile(r:Op[IntRaster], path:String, breaks:Array[(Int, Int)],
                        noDataColor:Int, transparent:Boolean) extends SimpleOp[Unit] {
  def _value(context:Context) = {
    val raster = context.run(r)
    val f = ColorMapper(ColorBreaks(breaks), noDataColor)
    val writer = new PNGRenderer(raster, path, f, noDataColor, transparent)
    writer.write
  }
}

case class WritePNGFile2(r:Op[IntRaster], path:String, noDataColor:Int, transparent:Boolean) extends SimpleOp[Unit] {
  def _value(context:Context) = {
    val writer = new PNGWriterRGB2(context.run(r), path, noDataColor, transparent)
    writer.write
  }
}
