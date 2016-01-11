package geotrellis.spark.ingest

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.vector._
import org.apache.spark.rdd._
import scala.reflect.ClassTag

object Tiler {
  def cutTiles[T, K: SpatialComponent: ClassTag, TileType: MergeView: CellGridPrototypeView: ClassTag] (
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, TileType)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout,
    resampleMethod: ResampleMethod = NearestNeighbor
  ): RDD[(K, TileType)] =
    rdd
      .flatMap { tup =>
        val (inKey, tile) = tup
        val extent = getExtent(inKey)
        mapTransform(extent)
          .coords
          .map  { spatialComponent =>
            val outKey = createKey(inKey, spatialComponent)
            val newTile = tile.prototype(cellType, tileLayout.tileCols, tileLayout.tileRows)
            (outKey, newTile.merge(mapTransform(outKey), extent, tile))
          }
       }

  def apply[T, K: SpatialComponent: ClassTag, TileType: MergeView: CellGridPrototypeView: ClassTag](
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, TileType)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout
  ): RDD[(K, TileType)] =
    apply(getExtent, createKey, rdd, mapTransform, cellType, tileLayout, NearestNeighbor)

  def apply[T, K: SpatialComponent: ClassTag, TileType: MergeView: CellGridPrototypeView: ClassTag](
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, TileType)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout,
    resampleMethod: ResampleMethod
  ): RDD[(K, TileType)] =
    cutTiles(getExtent, createKey, rdd, mapTransform, cellType, tileLayout, resampleMethod)
      .reduceByKey { case (tile1, tile2) =>
        tile1.merge(tile2)
      }

  def apply[T, K: SpatialComponent: ClassTag, TileType: (? => MergeMethods[TileType]): (? => CellGridPrototype[TileType]): ClassTag]
    (getExtent: T=> Extent, createKey: (T, SpatialKey) => K)
    (rdd: RDD[(T, TileType)], metaData: RasterMetaData, resampleMethod: ResampleMethod): RDD[(K, TileType)] = {
    apply(getExtent, createKey, rdd, metaData.mapTransform, metaData.cellType, metaData.tileLayout, resampleMethod)
  }

  def apply[T, K, TileType](rdd: RDD[(T, TileType)], metaData: RasterMetaData)(implicit tiler: Tiler[T, K, TileType]): RDD[(K, TileType)] =
    apply(rdd, metaData, NearestNeighbor)

  def apply[T, K, TileType](rdd: RDD[(T, TileType)], metaData: RasterMetaData, resampleMethod: ResampleMethod)(implicit tiler: Tiler[T, K, TileType]): RDD[(K, TileType)] = {
    tiler(rdd, metaData, resampleMethod)
  }
}
