package geotrellis.vector.voronoi

import geotrellis.vector._
import scala.collection.mutable.Map
import org.apache.commons.math3.linear._
import scala.math.{abs,sqrt}

class Voronoi(verts: Array[Point], extent: Extent) {
  
  object V2 {
    def apply(x: Double, y: Double) = { new V2(MatrixUtils.createRealVector(Array(x,y))) }
    def apply(p: Point) = { new V2(MatrixUtils.createRealVector(Array(p.x, p.y))) }
  }
  case class V2 (v: RealVector) {
    def -(that: V2) = V2(v subtract that.v)
    def +(that: V2) = V2(v add that.v)
    def *(s: Double) = V2(v mapMultiply s)
    def dot(that: V2): Double = v dotProduct that.v
    def length() = { sqrt(v dotProduct v) }
    def normalize() = {
      val len2 = this dot this
      if (abs(len2) < 1e-16)
        this * (1/sqrt(len2))
      else
        V2(0,0)
    }
    def x() = v.getEntry(0)
    def y() = v.getEntry(1)
    override def toString() = { s"($x,$y)" }
    def toPoint() = Point(x, y)
  }

  val dt = new Delaunay(verts)

  def rayLineIntersection(base: V2, normal: V2, a: V2, b: V2): Option[Point] = {
    val num = normal dot (a-base)
    val den = normal dot (a-b)
    
    if (abs(den) < 1e-16) {
      // normal is perpendicular to edge
      if (abs(num) < 1e-16) {
        // base lies on edge => base is intersection
        val x = b - a
        val y = base - a
        val r = (x dot y) / (x dot x)
        if (0 <= r && r < 1)
          Some(base.toPoint)
        else
          None
      } else {
        // base not on edge => no intersection
        None
      }
    } else {
      val t = num / den

      if (0 <= t && t < 1) {
        val pt = a + (b - a) * t
        val dir = V2(-normal.y,normal.x)
        if ((pt - base).dot(dir) >= 0)
          Some(pt.toPoint)
        else
          None
      } else
        None
    }
  }

  def rayExtentIntersection(base: V2, normal: V2): (Point,Int) = {
    def rli(a: V2, b: V2) = { rayLineIntersection(base, normal, a, b) }
    val ur = V2(extent.xmax,extent.ymax)
    val ul = V2(extent.xmin,extent.ymax)
    val ll = V2(extent.xmin,extent.ymin)
    val lr = V2(extent.xmax,extent.ymin)
    val intersection = (rli(ur,ul), rli(ul,ll), rli(ll,lr), rli(lr,ur))

    intersection match {
      case (None,None,None,None) => throw new java.lang.IllegalArgumentException(s"Point $base is outside of $extent in rayExtentIntersection()")
      case (Some(x),_,_,_) => (x,0)
      case (_,Some(x),_,_) => (x,1)
      case (_,_,Some(x),_) => (x,2)
      case (_,_,_,Some(x)) => (x,3)
    }
  }

  def rayExtentIntersection(e: HalfEdge[Int,Point]): (Point,Int) = {
    val to = verts(e.vert)
    val from = verts(e.src)
    rayExtentIntersection((V2(to) + V2(from)) * 0.5, V2(to) - V2(from))
  }

  def mkVoronoiCell(incidentEdge: HalfEdge[Int,Point]): Polygon = {
    var e = incidentEdge.flip
    var accum: List[Point] = Nil

    do {
      if (e.face == None) {
        val (bound1,edgeNum1) = rayExtentIntersection(e)
        val (bound2,edgeNum2) = rayExtentIntersection(e.prev)

        accum = bound1 :: accum
        var i = edgeNum1
        while (i != edgeNum2) {
          i match {
            case 0 => accum = Point(extent.xmin,extent.ymax) :: accum
            case 1 => accum = Point(extent.xmin,extent.ymin) :: accum
            case 2 => accum = Point(extent.xmax,extent.ymin) :: accum
            case 3 => accum = Point(extent.xmax,extent.ymax) :: accum
          }
          i = (i+1)%4
        }
        accum = bound2 :: accum
      } else {
        accum = e.face.get :: accum        
      }
      e = e.rotCCWSrc
    } while (e != incidentEdge.flip)

    Polygon(Line(accum.reverse).closed)
  }

  def mkVoronoiCell(i: Int): Polygon = {
    mkVoronoiCell(dt.faceIncidentToVertex(i))
  }

  def voronoiCells(): Iterator[Polygon] = {
    dt.faceIncidentToVertex.keysIterator.map(mkVoronoiCell(_))
  }

  def voronoiCellsWithPoints(): Iterator[(Polygon,Point)] = {
    dt.faceIncidentToVertex.keysIterator.map{ i:Int => (mkVoronoiCell(i),verts(i)) }
  }

}
