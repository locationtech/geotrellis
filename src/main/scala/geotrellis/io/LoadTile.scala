package geotrellis.io

import geotrellis._

import geotrellis.process._

object LoadTile {
  def apply(n:Op[String],col:Op[Int],row:Op[Int]):LoadTile =
    LoadTile(n,col,row,None)
}

case class LoadTile(n:Op[String],col:Op[Int],row:Op[Int],targetExtent:Op[Option[RasterExtent]]) 
    extends Op[Raster] {
  def _run(context:Context) = runAsync(List(n, col, row, targetExtent, context))
  val nextSteps:Steps = {
    case (n:String) :: (col:Int) :: (row:Int) :: (te:Option[RasterExtent]) :: (context:Context) :: Nil =>
      val layer = context.getRasterLayer(n)
      Result(layer.getTile(col,row,te))
  }
}
