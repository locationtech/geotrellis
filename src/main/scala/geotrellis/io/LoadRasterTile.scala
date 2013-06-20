package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
 * Load a specific tile from a raster at a given row and column.
 */
case class LoadRasterTile(n:Op[String], col:Op[Int], row:Op[Int]) extends Op[Raster] {
  def _run(context:Context) = runAsync(List(n, col, row, context))
  val nextSteps:Steps =  {
    case (n:String) :: (col:Int) :: (row:Int) :: (context:Context) :: Nil =>
      Result(getTileFromRasterLayer(context.getRasterLayer(n),col,row))
  }

  def getTileFromRasterLayer(rasterLayer:RasterLayer, col:Int, row:Int):Raster = {
    rasterLayer match {
      case layer:TileSetRasterLayer    => layer.getTile(col, row)
      case _ => throw new Exception("LoadRasterTile must operate on a tiled raster")
    }
  }
}
