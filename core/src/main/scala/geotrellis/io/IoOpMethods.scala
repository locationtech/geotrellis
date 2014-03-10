package geotrellis.io

import geotrellis._
import geotrellis.data._
import geotrellis.source._

trait IoOpMethods[+Repr <: RasterSource] { self: Repr =>
  def writePng(path:String) = 
    self.converge.mapOp(WritePngRgba(_,path))
}
