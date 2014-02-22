package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

trait Geometry {

  val geom: jts.Geometry

  def centroid: Point = Point(geom.getCentroid)

  def interiorPoint: Point = Point(geom.getInteriorPoint)

  def coveredBy(other: Geometry) =
    geom.coveredBy(other.geom)

  def covers(other: Geometry) =
    geom.covers(other.geom)

  def intersects(other: Geometry) =
    geom.intersects(other.geom)

  def disjoint(other: Geometry) =
    geom.disjoint(other.geom)

  def touches(other: Geometry) =
    geom.touches(other.geom)

  def distance(other: Geometry) =
    geom.distance(other.geom)

  // Curious to benchmark this against .distance < d,
  // JTS implements it as a different op, I'm assuming
  // for speed.
  def withinDistance(other: Geometry, dist: Double) =
    geom.isWithinDistance(other.geom, dist)

  /* TO BE IMPLEMENTED ON A PER TYPE BASIS */







  // equal (with tolerance?)
  // equalExact (huh?)
  // normalize (hmmm)




  // isValid ( don't allow invalid? )


  // symDifference - can't have a GC as an arg. May throw a TopologyException - how to deal with this?
  //    -done for point, line, and polygon combinations - still need to include multis


  // something with relate if it's fast (benchmark)

  /* IMPLEMENTED */

  // boundary
  // intersection ( & )
  // union ( | )
  // difference ( - )

  // crosses
  // within
  // contains - opposite of within

  // vertices - line, polygon; doesn't make much sense for a point
  // envelope - line, polygon; again, doesn't make sense for points since it just returns the point
  // boundingBox - same thing as envelope
  // length - line; points have length 0.0
  // perimeter - length of a polygon

  // isSimple - always true for valid polygons and empty geoms; true for points as well; false for PointSets with repeated points
  // overlaps - geoms must have same dimension and not all points in common and intersection of interiors has same dimension as geoms themselves - done for L/L and P/P


  // buffer - None on collections, always a polygon. (wait maybe on Multli's)
  // contains - Not on collections (wait maybe on Multli's) - if not, then other Geometry methods don't belong.
  // isRectangle (polygon)
  // def area:Double = geom.getArea  (not for points?)
  

  // def boundary = jts.getBoundary
  // def boundaryDimension = jts.getBoundaryDimension
  // def centroid = jts.getCentroid
  // def coordinate:(Double,Double) = jts.getCoordinate
  // def coordinates:Seq[(Double,Double)] = jts.getCoordinates
  // def dimension = jts.getDimension
}
