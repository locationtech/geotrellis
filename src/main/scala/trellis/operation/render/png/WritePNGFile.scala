package trellis.operation.render.png

import trellis._
import trellis.data._
import trellis.operation._
import trellis.process._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
//TODO: rename this to RenderPNGFile?
case class WritePNGFile(r:Op[IntRaster], path:Op[String], breaks:Op[Array[(Int, Int)]],
                        noDataColor:Op[Int], transparent:Op[Boolean]) 
extends Op5(r,path,breaks,noDataColor,transparent) ({
    (r,path,breaks,noDataColor,transparent) => {
    val f = ColorMapper(ColorBreaks(breaks), noDataColor)
    val writer = new PNGRenderer(r, path, f, noDataColor, transparent)
    writer.write
    Result(Unit)
  }
})

//TODO: rename this to WritePNGFile?
case class WritePNGFile2(r:Op[IntRaster], path:String, noDataColor:Int, transparent:Boolean) 
extends Op4(r,path,noDataColor,transparent)({
  (r,path,noDataColor,transparent) => {
    val writer = new PNGWriterRGB2(r, path, noDataColor, transparent)
    writer.write
    Result(Unit)
  }
})
