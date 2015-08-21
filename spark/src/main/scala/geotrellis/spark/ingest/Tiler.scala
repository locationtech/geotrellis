package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.vector._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import scala.reflect.ClassTag

object Tiler {
  def cutTiles[T, K: SpatialComponent: ClassTag, TileType: BlankTile] (
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, TileType)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout
  )(implicit ev: TileType => MergeTile[TileType]): RDD[(K, TileType)] =
    rdd
      .flatMap { tup =>
        val (inKey, tile) = tup
        val extent = getExtent(inKey)
        mapTransform(extent)
          .coords
          .map  { spatialComponent =>
            val outKey = createKey(inKey, spatialComponent)
            val tmsTile: TileType = implicitly[BlankTile[TileType]].makeFrom(tile, cellType, tileLayout.tileCols, tileLayout.tileRows)
            tmsTile.merge(mapTransform(outKey), extent, tile)
            tmsTile.merge(tile)
            (outKey, tmsTile)
          }
       }

  def apply[T, K: SpatialComponent: ClassTag, TileType: BlankTile: ClassTag](
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, TileType)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout
  )(implicit ev: TileType => MergeTile[TileType]): RDD[(K, TileType)] =
    cutTiles(getExtent, createKey, rdd, mapTransform, cellType, tileLayout)
      .reduceByKey { case (tile1: TileType, tile2: TileType) =>
        tile1.merge(tile2)
      }

  def apply[T, K: SpatialComponent: ClassTag, TileType: BlankTile]
    (getExtent: T=> Extent, createKey: (T, SpatialKey) => K)
    (rdd: RDD[(T, Tile)], metaData: RasterMetaData)
      : RasterRDD[K] = {
    val tiles = apply(getExtent, createKey, rdd, metaData.mapTransform, metaData.cellType, metaData.tileLayout)
    new RasterRDD(tiles, metaData)
  }

//  def apply[T: IngestKey, K: SpatialComponent: ClassTag]
//    (rdd: RDD[(T, Tile)], metaData: RasterMetaData)
//    (createKey: (T, SpatialKey) => K)
//      : RasterRDD[K] = {
//    val getExtent = (inKey: T) => inKey.projectedExtent.extent
//    val tiles = apply(getExtent, createKey)(rdd, metaData)
//    new RasterRDD(tiles, metaData)
//  }

//  def apply[K: SpatialComponent: ClassTag]
//    ( rdd: RDD[(Extent, Tile)],
//      metaData: RasterMetaData,
//      createKey: (Extent, SpatialKey) => K
//    ): RasterRDD[K] =
//  {
//    val getExtent = (inKey: Extent) => inKey
//    val tiles = apply(getExtent, createKey)(rdd, metaData)
//    new RasterRDD(tiles, metaData)
//  }

}
