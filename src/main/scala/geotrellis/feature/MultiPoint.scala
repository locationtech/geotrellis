package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }

class MultiPoint[D](override val geom:jts.MultiPoint, data:D) extends GeometryCollection(geom,data) {

  def flatten:List[Point[D]] =
    (0 until geom.getNumGeometries).map(
      i => new JtsPoint(geom.getGeometryN(i).asInstanceOf[jts.Point],data)).toList

}

/// MultiPoint implementation
object MultiPoint {
  val factory = Feature.factory

  /**
   * Create an empty MultiPoint feature.
   */
  def emtpy():MultiPoint[_] = 
    JtsMultiPoint(factory.createMultiPoint(Array[jts.Coordinate]()), None)

  /**
   * Create an empty MultiPoint feature with data.
   *
   * @param   data  Data of this feature
   */
  def empty[D](data: D):MultiPoint[D] = 
    JtsMultiPoint(factory.createMultiPoint(Array[jts.Coordinate]()), data)

  /**
   * Create a MultiPoint feature.
   *
   * @param   g     JTS MutliPoint object
   * @param   data  Data of this feature
   */
  def apply[D](g: jts.MultiPoint, data: D):JtsMultiPoint[D] = 
    JtsMultiPoint(g, data)

  /**
   * Create a MultiPoint feature with sequences of coordinate values.
   *
   * The coordinate values are represented as a sequence of coordinates, each
   * represented as a sequence of two double values (x and y).
   *
   * @param coords    Sequence of x and y sequences
   * @param data      The data of this feature
   */
  def apply[D](coords: Seq[Seq[Double]], data: D):JtsMultiPoint[D] = {
    val jtsMP = factory.createMultiPoint (
      coords.map ( coord => { 
        new jts.Coordinate(coord(0),coord(1))}).toArray
    )
    MultiPoint(jtsMP, data)    
  }
}

case class JtsMultiPoint[D](g: jts.MultiPoint, d: D) extends MultiPoint[D](g, d)
