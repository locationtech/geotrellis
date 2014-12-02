package geotrellis.spark.io

import geotrellis.spark._

import scala.reflect.ClassTag

import scala.util.Try

trait Catalog {
  type Params
  type SupportedKey[K]

  def metaDataCatalog: MetaDataCatalog[Params]

  def paramsFor[K: SupportedKey: ClassTag](layerId: LayerId): Params

  def load[K: SupportedKey: ClassTag](id: LayerId): Try[RasterRDD[K]] =
    load(id, new FilterSet[K])

  def load[K: SupportedKey: ClassTag](id: LayerId, params: Params): Try[RasterRDD[K]] =
    load(id, params, new FilterSet[K])

  def load[K: SupportedKey: ClassTag](id: LayerId, filters: FilterSet[K]): Try[RasterRDD[K]] =
    metaDataCatalog.load(id).flatMap { case (metaData, params) =>
      load(id, metaData, params, filters)
    }

  def load[K: SupportedKey: ClassTag](id: LayerId, params: Params, filters: FilterSet[K]): Try[RasterRDD[K]] = {
    metaDataCatalog.load(id, params).flatMap { metaData: RasterMetaData =>
      load(id, metaData, params, filters)
    }
  }

  def load[K: SupportedKey: ClassTag](id: LayerId, metaData: RasterMetaData, params: Params, filters: FilterSet[K]): Try[RasterRDD[K]]

  def save[K: SupportedKey: ClassTag](id: LayerId, rdd: RasterRDD[K]): Try[Unit] =
    save(id, rdd, false)

  def save[K: SupportedKey: ClassTag](id: LayerId, rdd: RasterRDD[K], clobber: Boolean): Try[Unit] =
    save(id, paramsFor(id), rdd, clobber)

  def save[K: SupportedKey: ClassTag](id: LayerId, params: Params, rdd: RasterRDD[K]): Try[Unit] =
    save(id, params, rdd, false)

  /** The implementer of this method is responsible for saving the metaData to the metaDataCatalog */
  def save[K: SupportedKey: ClassTag](id: LayerId, params: Params, rdd: RasterRDD[K], clobber: Boolean): Try[Unit]
}
