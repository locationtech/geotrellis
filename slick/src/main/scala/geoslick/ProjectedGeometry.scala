package geotrellis.slick

import geotrellis.proj4._
import geotrellis.feature._

sealed trait ProjectedGeometry{
  type G <: Geometry
  val srid: Int  
  val geom: G
}

object ProjectedGeometry {
  implicit def toGeometry(pg: ProjectedGeometry) = pg.geom

  def apply[T <: ProjectedGeometry](geom: Geometry, srid: Int) = {
    geom match {
      case g: Point => ProjectedPoint(g, srid)
      case g: Line => ProjectedLine(g, srid)
      case g: Polygon => ProjectedPolygon(g, srid)
      case g: GeometryCollection => ProjectedGeometryCollection(g, srid)
    }
  }
}

case class ProjectedPoint(geom: Point, srid: Int) extends ProjectedGeometry { 
  type G = Point 
}

case class ProjectedLine(geom: Line, srid: Int) extends ProjectedGeometry { 
  type G = Line 
}

case class ProjectedPolygon(geom: Polygon, srid: Int) extends ProjectedGeometry { 
  type G = Polygon 
}

case class ProjectedGeometryCollection(geom: GeometryCollection, srid: Int) extends ProjectedGeometry { 
  type G = GeometryCollection
}