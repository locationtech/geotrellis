package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

case class Intersect[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.mapGeom(_.intersection(other.geom)))
})

case class IntersectWithDataFunction[A,B,C](g:Op[Geometry[A]], other:Op[Geometry[B]], f:(A,B) => C) extends Op2(g,other) ({
  (g,other) => Result(Feature(g.geom.intersection(other.geom), f(g.data, other.data)))
})

/*
case class IntersectDim0DimX[A](f:Op[Geometry[A] with Dim2], other:Op[Geometry[_]]) extends Op2(f,other) ({
  (f,other) => Result(MultiPolygon(f.geom.intersection(other.geom), f.data))
}) 

case class IntersectDimXDim0[A](f:Op[Geometry[A]], other:Op[Geometry[_] with Dim2]) extends Op2(f,other) ({
  (f,other) => Result(MultiPolygon(f.geom.intersection(other.geom), f.data))
}) 
*/

object Intersect {
/*
  def apply[A](g:Op[Geometry[A] with Dim2], o:Op[Geometry[_]]) = {
    println("intersect apply: 1st argument Dim2")
    IntersectDim2DimX(g,o)
  }
  def apply[A](g:Op[Geometry[A]], o:Op[Geometry[_] with Dim2]) = {
    println("intersect apply: 2nd argument Dim2")
    IntersectDimXDim2(g,o)
  }
*/
  /*def apply[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) = {
     println("intersect apply: didn't see any Dim2")
     new Intersect(g,other,5)
  }*/
  def apply[A:Manifest,B:Manifest,C:Manifest](g:Op[Geometry[A]], other:Op[Geometry[B]], f:(A,B) => C) = 
    IntersectWithDataFunction(g,other,f)
}

/* def apply[A](g:Op[Geometry[A] with Dim1], o:Op[Geometry[_]]):Op[MultiLineString[A]] = AsMultiLineString(Intersect(g,o)) def apply[A](g:Op[Geometry[A]], o:Op[Geometry[_] with Dim1]):Op[MultiLineString[A]] = AsMultiLineString(Intersect(g,o)) 
  def apply[A](g:Op[Geometry[A] with Dim0], o:Op[Geometry[A] with Dim0]):Op[MultiPoint[A]] = AsMultiPoint(Intersect(g,o))
*/
/*
case class AsMultiPolygon[A](f:Op[Feature[A]]) extends Op1(f)({
  f => Result(MultiPolygon(f.geom.asInstanceOf[geom.MultiPolygon],f.data))
}) 

case class AsMultiLineString[A](f:Op[Feature[A]]) extends Op1(f)({
  f => Result(MultiLineString(f.geom.asInstanceOf[geom.MultiLineString],f.data))
})
 
case class AsMultiPoint[A](f:Op[Feature[A]]) extends Op1(f)({
  f => Result(MultiPoint(f.geom.asInstanceOf[geom.MultiPoint],f.data))
}) 

*/
