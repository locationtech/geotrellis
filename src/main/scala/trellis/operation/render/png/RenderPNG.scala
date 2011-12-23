package trellis.operation.render.png

import trellis.process.Server
import trellis.operation._
import trellis.data.PNGWriterRGB;

/**
  * Render a PNG of a raster given a set of color breaks specified as tuples of maximum   * value and an integer color value.  The background can be set to a specified color
  * or be made transparent.
  */
case class RenderPNG(r:IntRasterOperation, _colorBreaks:Array[(Int, Int)],
                     noDataColor:Int,
                     transparent:Boolean) extends PNGOperation with PNGBase 
                                                               with SimpleOperation[Array[Byte]] {
  val colorBreaks = _colorBreaks.sortWith(_._1 < _._1)
  def _value(server:Server) = {
    val raster = server.run(r)
    val writer = new PNGWriterRGB(raster, "/dev/null", applyColorMap,
                                  noDataColor, transparent)
    writer.render
  }
}

