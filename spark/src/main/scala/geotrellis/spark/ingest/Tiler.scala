package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.vector._
import org.apache.spark.rdd._
import scala.reflect.ClassTag

object Tiler {
  /**
   * This is a Tiler function constructor that will mosaic and tile an RDD of tiles that have an Extent.
   * Partial application of this function, given getExtent and createKey, produces a [[Tiler]].
   *
   * @param getExtent function to extract extent from source key T
   * @param createKey function to create target key, K, given corresponding source key, T, and a [[SpatialComponent]]
   * @param rdd       rdd containing source tiles, may be overlapping
   * @param metaData  RasterMetaData result tiles
   * @tparam T        type of input tile key
   * @tparam K        type of output tile key, must have SpatialComponent
   * @return          RasterRDD[K] of mosaicked tiles
   */
  def apply[T, K: SpatialComponent: ClassTag]
    (getExtent: T=> Extent, createKey: (T, SpatialKey) => K)
    (rdd: RDD[(T, Tile)], metaData: RasterMetaData)
  : RasterRDD[K] = {

    val bcMetaData = rdd.sparkContext.broadcast(metaData)

    val tilesWithKeys: RDD[(K, (K, Extent, Tile))] =
      rdd
        .flatMap { { tup =>
        val (inKey, tile) = tup
        val metaData = bcMetaData.value
        val extent = getExtent(inKey)
        metaData.mapTransform(extent)
          .coords
          .map  { spatialComponent =>
          val outKey = createKey(inKey, spatialComponent)
          (outKey, (outKey, extent, tile))
        } }
      }

    // Functions for combine step
    val createTile =  { (tup: (K, Extent, Tile)) => {
      val (key, extent, tile) = tup
      //val metaData = bcMetaData.value
      val tmsTile = ArrayTile.empty(metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
      tmsTile.merge(metaData.mapTransform(key), extent, tile)
    }}

    val combineTiles1 =  { (tile: MutableArrayTile, tup: (K, Extent, Tile)) => {
      val (key, extent, prevTile) = tup
      val metaData = bcMetaData.value
      tile.merge(metaData.mapTransform(key), extent, prevTile)
    }}

    val combineTiles2 = { (tile1: MutableArrayTile, tile2: MutableArrayTile) =>
      tile1.merge(tile2)
    }

    val newRdd: RDD[(K, Tile)] =
      new PairRDDFunctions(tilesWithKeys)
        .combineByKey(createTile, combineTiles1, combineTiles2)
        .map { case (key, tile) => (key, tile: Tile) }

    new RasterRDD[K](newRdd, metaData)
  }

  def apply[T: IngestKey, K: SpatialComponent: ClassTag]
    (rdd: RDD[(T, Tile)],metaData: RasterMetaData)
    (createKey: (T, SpatialKey) => K)
  : RasterRDD[K] = {
    val getExtent = (inKey: T) => inKey.projectedExtent.extent
    apply(getExtent, createKey)(rdd, metaData)
  }

  def apply[K: SpatialComponent: ClassTag]
    ( rdd: RDD[(Extent, Tile)],
      metaData: RasterMetaData,
      createKey: (Extent, SpatialKey) => K
    ): RasterRDD[K] =
  {
    val getExtent = (inKey: Extent) => inKey
    apply(getExtent, createKey)(rdd, metaData)
  }

}
