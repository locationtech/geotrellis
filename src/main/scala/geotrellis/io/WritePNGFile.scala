package geotrellis.op.render.png

import geotrellis._
import geotrellis._
import geotrellis.data._
import geotrellis.data.png._
import geotrellis.process._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
//TODO: rename this to RenderPNGFile?
case class WritePNGFile(r:Op[Raster], path:Op[String], breaks:Op[Array[(Int, Int)]],
                        noDataColor:Op[Int], transparent:Op[Boolean]) 
extends Op5(r,path,breaks,noDataColor,transparent) ({
    (r, path, breaks, noDataColor, transparent) => {
    val f = ColorMapper(ColorBreaks(breaks), noDataColor)
    RgbaEncoder().writePath(path, r.map(f))
    Result(Unit)
  }
})

//TODO: rename this to WritePNGFile?
case class WritePNGFile2(r:Op[Raster], path:String, noDataColor:Int, transparent:Boolean) 
extends Op4(r,path,noDataColor,transparent)({
  (r, path, noDataColor, transparent) => {
    RgbaEncoder().writePath(path, r)
    Result(Unit)
  }
})
