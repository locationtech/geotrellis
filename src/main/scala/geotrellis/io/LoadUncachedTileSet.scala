package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
 * Load a set of tiles from a specified directory, without caching the tiles into memory. 
 *
 * This directory must contain a layout.json file as well as tiles written
 * in the ARG format.
 */
case class LoadUncachedTileSet(path:Op[String]) extends Op[Raster] {
  def _run(context:Context) = runAsync(path :: context :: Nil)
  val nextSteps:Steps = {
    case (p:String) :: (c:Context) :: Nil => Result(c.loadUncachedTileSet(p))
  }
}
