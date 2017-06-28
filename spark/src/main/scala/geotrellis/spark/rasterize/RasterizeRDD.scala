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
  /** Rasterize geometries using a constant value */
  def fromGeometry[T: Numeric](
    geoms: RDD[Geometry],
    layout: LayoutDefinition,
    ct: CellType,
    value: T,
    options: Options
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    fromGeometry(geoms, layout, ct, value, new HashPartitioner(geoms.sparkContext.defaultParallelism), options)
  }

  /** Rasterize geometries using a constant value */
  def fromGeometry[T: Numeric](
    geoms: RDD[Geometry],
    layout: LayoutDefinition,
    ct: CellType,
    value: T,
    options: Options,
    numPartitions: Int
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    fromGeometry(geoms, layout, ct, value, new HashPartitioner(numPartitions), options)
  }

  /** Rasterize geometries using a constant value */
  def fromGeometry[T: Numeric](
    geoms: RDD[Geometry],
    layout: LayoutDefinition,
    ct: CellType,
    value: T,
    partitioner: Partitioner,
    options: Options
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val intValue = implicitly[Numeric[T]].toInt(value)
    val dblValue = implicitly[Numeric[T]].toDouble(value)
    val options = Options(includePartial=true, sampleType=PixelIsArea)
    val rasterExtent = RasterExtent(layout.extent, layout.layoutCols, layout.layoutRows)

    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, (Geometry, RasterExtent))] =
      geoms.flatMap { geom =>
        var buff = Map.empty[SpatialKey, (Geometry, RasterExtent)]
        geom.foreach(rasterExtent, options){ (col, row) =>
          val key = SpatialKey(col, row)
          val keyRasterExtent = RasterExtent(layout.mapTransform(key), layout.tileCols, layout.tileRows)
          // have to check because MultiLine can cause repeat visits from same geometry
          if (! buff.contains(key))
            buff = buff.updated(key, (geom, keyRasterExtent))
        }
        buff.toSeq
      }

    val createTile = (value: (Geometry, RasterExtent)) => {
      val (geom, re) = value
      val tile = ArrayTile.empty(ct, re.cols, re.rows)
      if (ct.isFloatingPoint)
        geom.foreach(re){ tile.setDouble(_, _, dblValue) }
      else
        geom.foreach(re){ tile.set(_, _, intValue) }
      tile: MutableArrayTile
    }

    val updateTile = (tile: MutableArrayTile, value: (Geometry, RasterExtent)) => {
      val (geom, re) = value
      if (ct.isFloatingPoint)
        geom.foreach(re){ tile.setDouble(_, _, dblValue) }
      else
        geom.foreach(re){ tile.set(_, _, intValue) }
      tile: MutableArrayTile
    }

    val mergeTiles = (t1: MutableArrayTile, t2: MutableArrayTile) => {
      t1.merge(t2).mutable
    }

    val tiles = keyed.combineByKeyWithClassTag[MutableArrayTile](
      createCombiner = createTile,
      mergeValue = updateTile,
      mergeCombiners = mergeTiles,
      partitioner
    )

    ContextRDD(tiles.asInstanceOf[RDD[(SpatialKey, Tile)]], layout)
  }
}
