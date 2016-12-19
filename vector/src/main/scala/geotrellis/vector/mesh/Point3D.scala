package geotrellis.vector.mesh

import geotrellis.vector.{Extent, Point}

case class Point3D(x: Double, y: Double, z: Double = 0.0) {
  def normalized(e: Extent): Point3D =
    Point3D(
      (x - e.xmin) / e.width,
      (y - e.ymin) / e.height,
      z
    )

  def distance2D(other: Point3D): Double = {
    val dx = other.x - x
    val dy = other.y - y
    math.sqrt(dx*dx + dy*dy)
  }

  def toPoint: Point =
    Point(x, y)
}
