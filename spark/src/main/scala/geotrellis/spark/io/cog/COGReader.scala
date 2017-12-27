package geotrellis.spark.io.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods

import java.net.URI

trait COGReader[V <: CellGrid] extends Serializable {
  implicit val tileMergeMethods: V => TileMergeMethods[V]
  implicit val tilePrototypeMethods: V => TilePrototypeMethods[V]
  implicit val tiffMethods: TiffMethods[V]

  def fullPath(path: String): URI

  val defaultThreads: Int
}
