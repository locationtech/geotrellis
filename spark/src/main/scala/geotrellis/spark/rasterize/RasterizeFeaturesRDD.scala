package geotrellis.spark.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import org.apache.spark.rdd._
import org.apache.spark.{HashPartitioner, Partitioner}

object RasterizeFeaturesRDD {

  /**
   * Rasterize an RDD of Geometry objects into a tiled raster RDD.
   * Cells not intersecting any geometry will left as NODATA.
   * Value will be converted to type matching specified [[CellType]].
   *
   * @param features Cell values for cells intersecting a feature consisting of (geometry,value)
   * @param layout Raster layer layout for the result of rasterization
   * @param cellType [[CellType]] for creating raster tiles
   * @param options Rasterizer options for cell intersection rules
   * @param partitioner Partitioner for result RDD
   */
  def fromFeature[G <: Geometry, D <: Double](
    features: RDD[Feature[G, D]],
    cellType: CellType,
    layout: LayoutDefinition,
    options: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val layoutRasterExtent = RasterExtent(layout.extent, layout.layoutCols, layout.layoutRows)
    val layoutRasterizerOptions = Rasterizer.Options(includePartial=true, sampleType=PixelIsArea)

    /** Key geometry by spatial keys of intersecting tiles */
    def keyGeom(feature: (Geometry, Double)): Iterator[(SpatialKey, ((Geometry, Double), SpatialKey))] = {
      var keySet = Set.empty[SpatialKey]
      feature._1.foreach(layoutRasterExtent, layoutRasterizerOptions){ (col, row) =>
        keySet = keySet + SpatialKey(col, row)
      }
      keySet.toIterator.map { key => (key, (feature, key)) }
    }

    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, ((Geometry, Double), SpatialKey))] =
      features.flatMap { feature => keyGeom(feature.geom, feature.data) }

    val createTile = (tup: ((Geometry, Double), SpatialKey)) => {
      val ((geom,value), key) = tup
      val tile = ArrayTile.empty(cellType, layout.tileCols, layout.tileRows)
      val re = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
      geom.foreach(re, options){ tile.setDouble(_, _, value) }
      tile: MutableArrayTile
    }

    val updateTile = (tile: MutableArrayTile, tup: ((Geometry, Double), SpatialKey)) => {
      val ((geom,value), key) = tup
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
      partitioner.getOrElse(new HashPartitioner(features.getNumPartitions))
    )

    ContextRDD(tiles.asInstanceOf[RDD[(SpatialKey, Tile)]], layout)
  }
}
