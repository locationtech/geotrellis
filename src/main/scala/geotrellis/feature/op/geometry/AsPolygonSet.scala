package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import geotrellis.{ op => liftOp }
import com.vividsolutions.jts.{ geom => jts }

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

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
