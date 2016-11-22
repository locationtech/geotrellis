package geotrellis.spark.io.pdal

import geotrellis.proj4.CRS

case class ProjectedPackedPointsBounds(bounds: PackedPointsBounds, crs: CRS)
