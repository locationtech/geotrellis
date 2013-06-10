package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Ensure that the result of the operation will be evaluated.
 * 
 * Some raster operations are lazily evaluated, which means that the
 * operations will defer their execution until the
 * moment where execution is necessary.  This allows, for example,
 * some raster operations to be combined and executed at the same
 * time instead of in sequence. 
 *
 * Force will evaluate a lazily evaluated operation if it has not 
 * yet been evaluated.
 * 
 */
case class Force[A](op:Op[A]) extends Op[A] {
  def _run(context:Context) = runAsync(op :: Nil)
  val nextSteps:Steps = {
    case (r:Raster) :: Nil => Result(r.force.asInstanceOf[A])
    case a :: Nil => Result(a.asInstanceOf[A])
  }
}
