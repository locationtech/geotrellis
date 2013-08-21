package geotrellis.io

import geotrellis._

import geotrellis.process._

case class LoadTile(n:Op[String],col:Op[Int],row:Op[Int]) extends Op[Raster] {
  def _run(context:Context) = runAsync(List(n, col, row, context))
  val nextSteps:Steps = {
    case (n:String) :: (col:Int) :: (row:Int) :: (context:Context) :: Nil =>
      val layer = context.getRasterLayer(n)
      Result(layer.getTile(col,row))
  }
}
