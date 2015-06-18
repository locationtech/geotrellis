package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.raster._

import scala.reflect.ClassTag

import org.apache.spark.rdd._

trait Reader[K, V] extends Function1[K,V]{
  def read(key: K): V
  def apply(key: K): V = read(key)
}

trait Writer[K, V] extends Function2[K,V,Unit] {
  def write(key: K, value: V): Unit
  def apply(key: K, value: V): Unit = write(key, value)
}

trait TileReader[K] extends Reader[K, geotrellis.raster.Tile]

trait FilterableRDDReader[L, K, V, R <: RDD[(K, V)]] extends Reader[L, R] { 
  type Filter[K,T]
  def read(rddKey: L): R
  def filter[T](paramsList: T*)(implicit filter: Filter[K, T]): FilterableRDDReader[L, K, V, R]
}

trait RasterRDDReader[K, T] extends Reader[LayerId, RasterRDD[K, T]]

trait RasterRDDWriter[K, T] extends Writer[LayerId, RasterRDD[K, T]]

trait Store[K, V] extends Reader[K, V] with Writer[K, V]
