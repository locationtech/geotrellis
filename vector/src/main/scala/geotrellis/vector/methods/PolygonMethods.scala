package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions
import org.locationtech.jts.geom.CoordinateSequence
import spire.syntax.cfor._

trait ExtraPolygonMethods extends MethodExtensions[Polygon] {
  def exterior: LineString =
    self.getExteriorRing.copy.asInstanceOf[LineString]

  def holes: Array[LineString] =
  (for (i <- 0 until self.getNumInteriorRing) yield self.getInteriorRingN(i).copy.asInstanceOf[LineString]).toArray

  private def populatePoints(sequence: CoordinateSequence, arr: Array[Point], offset: Int = 0): Array[Point] = {
    cfor(0)(_ < sequence.size, _ + 1) { i =>
      arr(i + offset) = Point(sequence.getX(i), sequence.getY(i))
    }

    arr
  }

  def vertices: Array[Point] = {
    val arr = Array.ofDim[Point](self.getNumPoints)

    val sequences = self.getExteriorRing.getCoordinateSequence +: (0 until self.getNumInteriorRing).map(self
      .getInteriorRingN(_).getCoordinateSequence)
    val offsets = sequences.map(_.size).scanLeft(0)(_ + _).dropRight(1)

    sequences.zip(offsets).foreach { case (seq, offset) => populatePoints(seq, arr, offset) }

    arr
  }
}
