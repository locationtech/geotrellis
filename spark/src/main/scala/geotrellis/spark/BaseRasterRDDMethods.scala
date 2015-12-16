package geotrellis.spark

import geotrellis.raster._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

trait BaseRasterRDDMethods[K] extends Serializable {
  implicit val keyClassTag: ClassTag[K]
  val rdd: RDD[(K, Tile)] with Metadata[RasterMetaData]
  val metaData = rdd.metadata

  def convert(cellType: CellType): RDD[(K, Tile)] with Metadata[RasterMetaData] =
    mapTiles(_.convert(cellType))

  def reduceByKey(f: (Tile, Tile) => Tile): RDD[(K, Tile)] with Metadata[RasterMetaData] =
    rdd.withContext { rdd => rdd.reduceByKey(f) }


  def mapKeys[R: ClassTag](f: K => R): RDD[(R, Tile)] with Metadata[RasterMetaData] =
    rdd.withContext { rdd => rdd.map { case (key, tile) => f(key) -> tile } }

  def mapTiles(f: Tile => Tile): RDD[(K, Tile)] with Metadata[RasterMetaData] =
    rdd.withContext { rdd => rdd.map { case (key, tile) => key -> f(tile) } }

  def mapPairs[R: ClassTag](f: ((K, Tile)) => (R, Tile)): RDD[(R, Tile)] with Metadata[RasterMetaData] =
    rdd.withContext { rdd => rdd map { row => f(row) } }

  def combineTiles(other: RDD[(K, Tile)] with Metadata[RasterMetaData])(f: (Tile, Tile) => Tile): RDD[(K, Tile)] with Metadata[RasterMetaData] =
    combinePairs(other) { case ((k1, t1), (k2, t2)) => (k1, f(t1, t2)) }

  def combinePairs[R: ClassTag](other: RDD[(K, Tile)])(f: ((K, Tile), (K, Tile)) => (R, Tile)): RDD[(R, Tile)] with Metadata[RasterMetaData] =
    rdd.withContext { rdd =>
      rdd.join(other).map { case (key, (tile1, tile2)) => f((key, tile1), (key, tile2)) }
    }

  def combinePairs(others: Traversable[RDD[(K, Tile)]])(f: (Traversable[(K, Tile)] => (K, Tile))): RDD[(K, Tile)] with Metadata[RasterMetaData] = {
    def create(t: (K, Tile)) = List(t)
    def mergeValue(ts: List[(K, Tile)], t: (K, Tile)) = ts :+ t
    def mergeContainers(ts1: List[(K, Tile)], ts2: Traversable[(K, Tile)]) = ts1 ++ ts2

    rdd.withContext { rdd =>
      (rdd :: others.toList)
        .reduceLeft(_ ++ _)
        .map(t => (t.id, t))
        .combineByKey(create, mergeValue, mergeContainers)
        .map { case (id, tiles) => f(tiles) }
    }
  }

  def asRasters()(implicit sc: SpatialComponent[K]): RDD[(K, Raster)] =
    rdd.mapPartitions({ part =>
      part.map { case (key, tile) =>
        (key, Raster(tile, metaData.mapTransform(key)))
      }
    }, true)

  def minMax: (Int, Int) =
    rdd.map(_.tile.findMinMax)
      .reduce { (t1, t2) =>
        val (min1, max1) = t1
        val (min2, max2) = t2
        val min =
          if(isNoData(min1)) min2
          else {
            if(isNoData(min2)) min1
            else math.min(min1, min2)
          }
        val max =
          if(isNoData(max1)) max2
          else {
            if(isNoData(max2)) max1
            else math.max(max1, max2)
          }
        (min, max)
      }

  def minMaxDouble: (Double, Double) =
    rdd.map(_.tile.findMinMaxDouble)
      .reduce { (t1, t2) =>
        val (min1, max1) = t1
        val (min2, max2) = t2
        val min =
          if(isNoData(min1)) min2
          else {
            if(isNoData(min2)) min1
            else math.min(min1, min2)
          }
        val max =
          if(isNoData(max1)) max2
          else {
            if(isNoData(max2)) max1
            else math.max(max1, max2)
          }
        (min, max)
      }
}
