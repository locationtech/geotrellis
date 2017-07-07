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
  case class Options(
    rasterizerOptions: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    partitioner: Option[Partitioner] = None
  )

  object Options {
    val DEFAULT = Options()
  }

  /** Rasterize geometries using a constant value */
  def fromGeometry[G <: Geometry](
    geoms: RDD[G],
    value: Double,
    layout: LayoutDefinition,
    ct: CellType,
    options: Options
  ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val intValue = d2i(value)
    val dblValue = value
    val rasterExtent = RasterExtent(layout.extent, layout.layoutCols, layout.layoutRows)
    val partitioner = options.partitioner.getOrElse(new HashPartitioner(geoms.getNumPartitions))

    // key the geometry to intersecting tiles so it can be rasterized in the map-side combine
    val keyed: RDD[(SpatialKey, (Geometry, RasterExtent))] =
      geoms.flatMap { geom =>
        var buff = Map.empty[SpatialKey, (Geometry, RasterExtent)]
        geom.foreach(rasterExtent, options.rasterizerOptions){ (col, row) =>
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

//   /** Rasterize geometries using a constant value */
//   def fromGeometry[G <: Geometry, T: Numeric](
//     geoms: RDD[G],
//     value: T,
//     layout: LayoutDefinition,
//     ct: CellType,
//     options: Options
//   ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
//     fromGeometry(geoms, layout, ct, value, new HashPartitioner(geoms.sparkContext.defaultParallelism), options)
//   }

//   /** Rasterize geometries using a constant value */
//   def fromGeometry[G <: Geometry, T: Numeric](
//     geoms: RDD[G],
//     value: T
//     layout: LayoutDefinition,
//     ct: CellType,
//   ): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
//     fromGeometry(geoms, layout, ct, value, new HashPartitioner(geoms.sparkContext.defaultParallelism), Options.DEFAULT)
//   }
}
