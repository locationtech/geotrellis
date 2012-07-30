package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
 * Load the a set of tiles the specified directory.
 *
 * This directory must contain a layout.json file as well as tiles written
 * in the ARG format.
 */
case class LoadTileSet(path:Op[String]) extends Op[Raster] {
  def _run(context:Context) = runAsync(path :: context :: Nil)
  val nextSteps:Steps = {
    case (p:String) :: (c:Context) :: Nil => Result(c.loadTileSet(p))
  }
}
