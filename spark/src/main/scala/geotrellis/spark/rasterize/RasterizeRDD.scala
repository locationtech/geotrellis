package geotrellis.spark.rasterize

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster.rasterize._
import geotrellis.raster.rasterize.Rasterizer.Options
import org.apache.spark.{HashPartitioner, Partitioner}
import org.apache.spark.rdd._
import scala.collection.immutable.VectorBuilder

object RasterizeRDD {

  /**
   * Rasterize an RDD of Geometry objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Value will be converted to type matching specified [[CellType]].
   *
   * @param value Cell value for cells intersecting a geometry
   * @param layout Raster layer layout for the result of rasterization
   * @param cellType [[CellType]] for creating raster tiles
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def fromGeometry[G <: Geometry](
    geoms: RDD[G],
    value: Double,
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val features = geoms.map({ g => Feature(g, value) })
    RasterizeFeaturesRDD.fromFeature(features, cellType, layout, options, partitioner);
  }
}
