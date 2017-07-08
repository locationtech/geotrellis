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
    val layoutRasterExtent = RasterExtent(layout.extent, layout.layoutCols, layout.layoutRows)
    val layoutRasterizerOptions = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)

    /** Key geometry by spatial keys of intersecting tiles */
    def keyGeom(geom: Geometry): Iterator[(SpatialKey, (Geometry, SpatialKey))] = {
      var keySet = Set.empty[SpatialKey]
      geom.foreach(layoutRasterExtent, layoutRasterizerOptions){ (col, row) =>
        keySet = keySet + SpatialKey(col, row)
      }
      keySet.toIterator.map { key => (key, (geom, key)) }
    }

    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, (Geometry, SpatialKey))] =
      geoms.flatMap { geom => keyGeom(geom) }

    val createTile = (tup: (Geometry, SpatialKey)) => {
      val (geom, key) = tup
      val tile = ArrayTile.empty(cellType, layout.tileCols, layout.tileRows)
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
      geom.foreach(re, options){ tile.setDouble(_, _, value) }
      tile: MutableArrayTile
    }

    val updateTile = (tile: MutableArrayTile, tup: (Geometry, SpatialKey)) => {
      val (geom, key) = tup
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
      geom.foreach(re, options){ tile.setDouble(_, _, value) }
      tile: MutableArrayTile
    }

    val mergeTiles = (t1: MutableArrayTile, t2: MutableArrayTile) => {
      t1.merge(t2).mutable
    }

    val tiles = keyed.combineByKeyWithClassTag[MutableArrayTile](
      createCombiner = createTile,
      mergeValue = updateTile,
      mergeCombiners = mergeTiles,
      partitioner.getOrElse(new HashPartitioner(geoms.getNumPartitions))
    )

    ContextRDD(tiles.asInstanceOf[RDD[(SpatialKey, Tile)]], layout)
  }
}
