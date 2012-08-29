package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._

/**
  * Computes a buffer area around this geometry.
  *
  * From the JTS documenation: "Computes a buffer area around this geometry having the given width. The buffer of a Geometry is the Minkowski sum or difference of the geometry with a disc of radius abs(distance)."
  *
  * @param detail How many line segments used to approximate a quarter circle (default 8) 
  */
//case class Buffer[A](f:Op[Geometry[A]], distance:Op[Double], detail:Op[Int]) extends Op3[Geometry[A],Double,Int,Polygon[A] with Dim2](f,distance,detail)  ({
case class Buffer[A](f:Op[Geometry[A]], distance:Op[Double], detail:Op[Int]) extends Op3(f,distance,detail)  ({
  (f,distance,detail) => Result(Polygon(f.geom.buffer(distance, detail),f.data))
})

object Buffer {
  def apply[D:Manifest](p:Op[Point[D]], dist:Op[Double]) = {
    logic.Do(p, dist) ( (p,d) => { Polygon(p.geom.buffer(d), p.data) } )
  }
}

