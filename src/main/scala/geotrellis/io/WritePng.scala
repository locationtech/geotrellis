package geotrellis.io

import geotrellis._
import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.process._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
case class WritePng(r:Op[Raster], path:Op[String],
                    colorBreaks:Op[Array[(Int, Int)]],
                    noDataColor:Op[Int], transparent:Op[Boolean]) 
extends Op5(r, path, colorBreaks, noDataColor, transparent) ({
    (r, path, colorBreaks, noDataColor, transparent) => {
    val breaks = colorBreaks.map(_._1)
    val colors = colorBreaks.map(_._2)
    val renderer = Renderer(breaks, colors, noDataColor)
    val r2 = renderer.render(r)
    val bytes = new Encoder(renderer.settings).writePath(path, r2)
    Result(())
  }
})
