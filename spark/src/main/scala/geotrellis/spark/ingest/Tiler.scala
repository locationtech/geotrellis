package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.vector._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import scala.reflect.ClassTag

/** Constructs a RasterRDD by cutting/merging tiles from sources */
object Tiler {

  /** Handles the actual construction or a RasterRDD by cutting and
    * merging tiles
    *
    *  @param getExtent Function that returns an extent for a tile
    *  @param createKey Function that returns a key given a tile and spatial key
    *  @param rdd RDD with tiles that need to potentially be merged
    *  @param mapTransform Function that transforms an extent to coordinates
    *  @param cellType type of cell (e.g. Double, Int, etc.)
    *  @param tileLayout layout of tiles for the RDD
    */
  def cutTiles[T, K: SpatialComponent: ClassTag] (
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, Tile)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout
  ): RDD[(K, Tile)] =
    rdd
      .flatMap { tup =>
        val (inKey, tile) = tup
        val extent = getExtent(inKey)
        mapTransform(extent)
          .coords
          .map  { spatialComponent =>
            val outKey = createKey(inKey, spatialComponent)
            val tmsTile = ArrayTile.empty(cellType, tileLayout.tileCols, tileLayout.tileRows)
            tmsTile.merge(mapTransform(outKey), extent, tile)

            (outKey, tmsTile)
          }
       }

  def apply[T, K: SpatialComponent: ClassTag](
    getExtent: T=> Extent,
    createKey: (T, SpatialKey) => K,
    rdd: RDD[(T, Tile)],
    mapTransform: MapKeyTransform,
    cellType: CellType,
    tileLayout: TileLayout
  ): RDD[(K, Tile)] =
    cutTiles(getExtent, createKey, rdd, mapTransform, cellType, tileLayout)
      .reduceByKey { case (tile1: Tile, tile2: Tile) =>
        tile1.merge(tile2)
      }

  def apply[T, K: SpatialComponent: ClassTag]
    (getExtent: T=> Extent, createKey: (T, SpatialKey) => K)
    (rdd: RDD[(T, Tile)], metaData: RasterMetaData)
      : RasterRDD[K] = {
    val tiles = apply(getExtent, createKey, rdd, metaData.mapTransform, metaData.cellType, metaData.tileLayout)
    new RasterRDD(tiles, metaData)
  }

  def apply[T: IngestKey, K: SpatialComponent: ClassTag]
    (rdd: RDD[(T, Tile)], metaData: RasterMetaData)
    (createKey: (T, SpatialKey) => K)
      : RasterRDD[K] = {
    val getExtent = (inKey: T) => inKey.projectedExtent.extent
    val tiles = apply(getExtent, createKey)(rdd, metaData)
    new RasterRDD(tiles, metaData)
  }

  def apply[K: SpatialComponent: ClassTag]
    ( rdd: RDD[(Extent, Tile)],
      metaData: RasterMetaData,
      createKey: (Extent, SpatialKey) => K
    ): RasterRDD[K] =
  {
    val getExtent = (inKey: Extent) => inKey
    val tiles = apply(getExtent, createKey)(rdd, metaData)
    new RasterRDD(tiles, metaData)
  }

}
