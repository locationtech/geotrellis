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

trait SimpleRasterRDDReader[K, RDDContainer[_]]{
  val defaultNumPartitions: Int
  def read(id: LayerId, numPartitions: Int): RDDContainer[K]

  def read(id: LayerId): RDDContainer[K] =
    read(id, defaultNumPartitions)
}

abstract class FilteringRasterRDDReader[K: Boundable, RDDContainer[_]] extends SimpleRasterRDDReader[K, RDDContainer] {
  def read(id: LayerId, rasterQuery: RasterRDDQuery[K], numPartitions: Int): RDDContainer[K]

  def read(id: LayerId, rasterQuery: RasterRDDQuery[K]): RDDContainer[K] =
    read(id, rasterQuery, defaultNumPartitions)

  def read(id: LayerId, numPartitions: Int): RDDContainer[K] =
    read(id, new RasterRDDQuery[K], numPartitions)

  def query(layerId: LayerId): BoundRasterRDDQuery[K, RDDContainer] =
    new BoundRasterRDDQuery(new RasterRDDQuery[K], read(layerId, _))

  def query(layerId: LayerId, numPartitions: Int): BoundRasterRDDQuery[K, RDDContainer] =
    new BoundRasterRDDQuery(new RasterRDDQuery[K], read(layerId, _, numPartitions))
}

trait Store[K, V] extends Reader[K, V] with Writer[K, V]