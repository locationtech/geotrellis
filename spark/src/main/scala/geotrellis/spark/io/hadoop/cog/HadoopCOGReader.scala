package geotrellis.spark.io.hadoop.cog

import geotrellis.raster.CellGrid
import geotrellis.spark.io.cog.{COGReader, TiffMethods}

trait HadoopCOGReader[V <: CellGrid] extends COGReader[V] {
  val tiffMethods: TiffMethods[V] with HadoopCOGBackend
}
