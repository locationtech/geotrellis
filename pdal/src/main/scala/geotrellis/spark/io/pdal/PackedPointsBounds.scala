package geotrellis.spark.io.pdal

case class PackedPointsBounds(
  maxx: Double,
  minx: Double,
  maxy: Double,
  miny: Double,
  maxz: Double,
  minz: Double,
  offset_x: Double,
  offset_y: Double,
  offset_z: Double,
  scale_x: Double,
  scale_y: Double,
  scale_z: Double
)
