package geotrellis.feature.op

import geotrellis._
import geotrellis.feature._
import geotrellis.{ op => liftOp }
import com.vividsolutions.jts.{ geom => jts }

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

package object geometry {

  val GetExtent = liftOp { (a: Raster) => a.rasterExtent.extent }
  val AsFeature = liftOp { (e: Extent) => e.asFeature(()) }

  /**
   * Given a Geometry object, inspect the underlying geometry type
   * and recursively flatten it if it is a GeometryCollection
   */
  case class FlattenGeometry[D](g1: Op[Geometry[D]]) extends Operation[List[Geometry[D]]] {

    def flattenGeometry(g: jts.Geometry):List[jts.Geometry] = g match {
        case g: jts.GeometryCollection => (0 until g.getNumGeometries).flatMap(
          i => flattenGeometry(g.getGeometryN(i))).toList
        case l: jts.LineString => List(l)
        case p: jts.Point => List(p)
        case p: jts.Polygon => List(p)
      }

    def _run(context:Context) = runAsync(List(g1))
    val nextSteps:Steps = {
      case a :: Nil => {
        val g = a.asInstanceOf[Geometry[D]]
        val geoms = flattenGeometry(g.geom)
        Result(geoms.map(geom => Feature(geom, g.data)))
      }
    }
  }

  /**
   * Given list of geometries, inspect the underlying geometry type,
   * selecting only geometries that match G
   */
  case class FilterGeometry[G <: jts.Geometry,D](s: Op[List[Geometry[D]]])(
    implicit t: TypeTag[G])
      extends Op1(s)( s => {
        val typeName = t.tpe.typeSymbol.name.toString()
        val result = s.filter(_.geom.getGeometryType == typeName)
        Result( result )
      })


  /**
   * Returns a Geometry as a Polygon Set.
   */
  case class AsPolygonSet[D](geom: Op[Geometry[D]]) extends Operation[List[Polygon[D]]] {    
    def flattenGeometry(g: jts.Geometry):List[jts.Polygon] = g match {
        case g: jts.GeometryCollection => (0 until g.getNumGeometries).flatMap(
          i => flattenGeometry(g.getGeometryN(i))).toList
        case l: jts.LineString => List()
        case p: jts.Point => List()
        case p: jts.Polygon => List(p)
      }

    def _run(context:Context) = runAsync(geom :: Nil)

    val nextSteps:Steps = {
      case a :: Nil => {
        val g = a.asInstanceOf[Geometry[D]]
        val geoms = flattenGeometry(g.geom)
        val r:List[Polygon[D]] = geoms.map(geom => Polygon(geom, g.data))
        Result(r)
      }
    }
  }


}
