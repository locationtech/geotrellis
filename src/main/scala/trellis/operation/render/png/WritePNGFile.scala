package trellis.operation.render.png

import trellis.process._
import trellis.operation._
import trellis.data.{PNGWriterRGB,PNGWriterRGB2}


/**
  * Write out a PNG graphic file to the file system at the specified path.
  */
case class WritePNGFile(r:IntRasterOperation, path:String,
                        colorBreaks:Array[(Int, Int)], noDataColor:Int,
                        transparent:Boolean) extends Operation[Unit] with PNGBase with SimpleOperation[Unit] {
  def _value(context:Context) = {
    val raster = context.run(r)
    val writer = new PNGWriterRGB(raster, path, applyColorMap, noDataColor,
                                  transparent)
    writer.write
  }
}

case class WritePNGFile2(r:IntRasterOperation, path:String, noDataColor:Int, transparent:Boolean)
extends Operation[Unit] with SimpleOperation[Unit] {
  def childOperations = { List(r) }

  def _value(context:Context) = {
    val raster = context.run(r)
    val writer = new PNGWriterRGB2(raster, path, noDataColor, transparent)
    writer.write
  }
}
