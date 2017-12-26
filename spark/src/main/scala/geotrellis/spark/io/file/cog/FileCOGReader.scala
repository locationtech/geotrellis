package geotrellis.spark.io.file.cog

import geotrellis.raster.CellGrid
import geotrellis.spark.io.cog.{COGReader, TiffMethods}

trait FileCOGReader[V <: CellGrid] extends COGReader[V] {
  val tiffMethods: TiffMethods[V] with FileCOGBackend
}
