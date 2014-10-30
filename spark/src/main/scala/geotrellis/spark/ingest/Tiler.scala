package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector._

import org.apache.spark.rdd._
import monocle.syntax._

import scala.reflect.ClassTag

abstract class Tiler[T, K: SpatialComponent: ClassTag] {
  def getExtent(inKey: T): Extent
  def createKey(inKey: T, spatialComponent: SpatialKey): K

  def tile(rdd: RDD[(T, Tile)], metaData: RasterMetaData): RasterRDD[K] = {
    val bcMetaData = rdd.sparkContext.broadcast(metaData)

    val tilesWithKeys: RDD[(K, (K, Extent, Tile))] =
      rdd
        .flatMap { case (inKey, tile) =>
          val metaData = bcMetaData.value
          val extent = getExtent(inKey)
          metaData.mapTransform(extent)
            .coords
            .map { spatialComponent =>
              val outKey = createKey(inKey, spatialComponent)
              (outKey, (outKey, extent, tile))
            }
         }

    // Functions for combine step
    def createTile(tup: (K, Extent, Tile)): MutableArrayTile = {
      val (key, extent, tile) = tup
      val metaData = bcMetaData.value
      val tmsTile = ArrayTile.empty(metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
      tmsTile.merge(metaData.mapTransform(key), extent, tile)
    }

    def combineTiles1(tile: MutableArrayTile, tup: (K, Extent, Tile)): MutableArrayTile = {
      val (key, extent, prevTile) = tup
      val metaData = bcMetaData.value
      tile.merge(metaData.mapTransform(key), extent, prevTile)
    }

    def combineTiles2(tile1: MutableArrayTile, tile2: MutableArrayTile): MutableArrayTile =
      tile1.merge(tile2)

    val newRdd: RDD[(K, Tile)] =
      new PairRDDFunctions(tilesWithKeys)
        .combineByKey(createTile, combineTiles1, combineTiles2)
        .map { case (key, tile) => (key, tile: Tile) }

    new RasterRDD[K](newRdd, metaData)
  }
}

object Tiler {
  def apply[T: IngestKey, K: SpatialComponent: ClassTag](createKey: (T, SpatialKey) => K): Tiler[T, K] = {
    val _projectedExtent = implicitly[IngestKey[T]]
    val ck = createKey

    new Tiler[T, K] {
      def getExtent(inKey: T): Extent = (inKey |-> _projectedExtent get).extent
      def createKey(inKey: T, spatialComponent: SpatialKey): K = ck(inKey, spatialComponent)
    }
  }

  def apply[K: SpatialComponent: ClassTag](createKey: (Extent, SpatialKey) => K): Tiler[Extent, K] = {
    val ck = createKey

    new Tiler[Extent, K] {
      def getExtent(inKey: Extent): Extent = inKey
      def createKey(inKey: Extent, spatialComponent: SpatialKey): K = ck(inKey, spatialComponent)
    }
  }
}
