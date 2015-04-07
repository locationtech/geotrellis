package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.raster._

import scala.reflect.ClassTag

import org.apache.spark.rdd._

trait Reader[K, V] {
  def read(key: K): V
}

trait Writer[K, V] {
  def write(key: K, value: V): Unit
}

trait TileReader[K] extends Reader[K, geotrellis.raster.Tile]

trait FilterableRDDReader[L, K, V, R <: RDD[(K, V)]] extends Reader[L, R] {
  def read(rddKey: L): R = read(rddKey, FilterSet.empty[K])

  def read(rddKey: L, filters: FilterSet[K]): R
}

trait RasterRDDReader[K] extends Reader[LayerId, RasterRDD[K]]

trait FilterableRasterRDDReader[K] extends FilterableRDDReader[LayerId, K, Tile, RasterRDD[K]]

trait RasterRDDWriter[K] extends Writer[LayerId, RasterRDD[K]]

trait Store[K, V] extends Reader[K, V] with Writer[K, V]
