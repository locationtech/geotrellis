package geotrellis.vector.voronoi

import geotrellis.vector._

import com.vividsolutions.jts.geom.{GeometryCollection, Coordinate, Point => JTSPoint, Polygon => JTSPolygon}

class Voronoi(val verts: Array[Point]) {

  val del = Delaunay(verts)

  def voronoiCells(): Seq[Polygon] = {
    val vd = del.subd.getVoronoiDiagram(del.gf).asInstanceOf[GeometryCollection]
    for ( i <- 0 until vd.getNumGeometries) yield Polygon(vd.getGeometryN(i).asInstanceOf[JTSPolygon])
  }

  def voronoiCellsWithPoints: Seq[(Polygon, Point)] = {
    val vd = del.subd.getVoronoiDiagram(del.gf).asInstanceOf[GeometryCollection]
    for ( i <- 0 until vd.getNumGeometries) yield {
      val jtspoly = vd.getGeometryN(i).asInstanceOf[JTSPolygon]
      val jtscenter = jtspoly.getUserData.asInstanceOf[Coordinate]
      (Polygon(jtspoly), Point.jtsCoord2Point(jtscenter))
    }
  }
}

