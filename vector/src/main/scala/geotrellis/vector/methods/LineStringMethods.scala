package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions
import org.locationtech.jts.geom.Coordinate
import spire.syntax.cfor._

trait ExtraLineStringMethods extends MethodExtensions[LineString] {
  def closed(): LineString = {
    val arr = Array.ofDim[Coordinate](self.getNumPoints + 1)

    cfor(0)(_ < arr.length, _ + 1) { i =>
      arr(i) = self.getCoordinateN(i)
    }
    arr(self.getNumPoints) = self.getCoordinateN(0)

    GeomFactory.factory.createLineString(arr)
  }

  def points: Array[Point] = {
    val arr = Array.ofDim[Point](self.getNumPoints)
    val sequence = self.getCoordinateSequence

    cfor(0)(_ < arr.length, _ + 1) { i =>
      arr(i) = Point(sequence.getX(i), sequence.getY(i))
    }

    arr
  }

  def typedIntersection(p: Point): PointOrNoResult = self.intersection(p)
  def typedIntersection(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(mp)
  def typedIntersection[G <: Geometry : AtLeastOneDimension](g: G): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(g)

  def normalized(): LineString = {
    val res = self.copy.asInstanceOf[LineString]
    res.normalize
    res
  }
}
