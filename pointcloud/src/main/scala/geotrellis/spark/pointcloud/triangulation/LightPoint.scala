package geotrellis.spark.pointcloud.triangulation

import geotrellis.vector.{Extent, Point}

case class LightPoint(x: Double, y: Double, z: Double = 0.0) {
  def normalized(e: Extent): LightPoint =
    LightPoint(
      (x - e.xmin) / e.width,
      (y - e.ymin) / e.height,
      z
    )

  def distance(other: LightPoint): Double = {
    val dx = other.x - x
    val dy = other.y - y
    math.sqrt(dx*dx + dy*dy)
  }

  def toPoint: Point =
    Point(x, y)
}
