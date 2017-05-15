package geotrellis.vector.mesh

trait PointSet[V] {
  def length: Int
  def getX(i: V): Double
  def getY(i: V): Double
  def getZ(i: V): Double
  def getPoint(i: V): Point3D
}
