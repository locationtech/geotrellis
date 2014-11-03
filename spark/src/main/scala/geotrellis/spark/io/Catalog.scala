package geotrellis.spark.io

import geotrellis.spark._

import scala.reflect.ClassTag

import scala.util.Try

trait Catalog {
  type Params
  type SupportedKey[K]

  def metaDataCatalog: MetaDataCatalog[Params]
  def paramsFor[K: SupportedKey: ClassTag](layerId: LayerId): Params

  def load[K: SupportedKey: Ordering: ClassTag](layerId: LayerId): Try[RasterRDD[K]] =
    load(layerId, FilterSet.EMPTY[K])

  def load[K: SupportedKey: Ordering: ClassTag](layerId: LayerId, filters: KeyFilter[K]*): Try[RasterRDD[K]] =
    load(layerId, FilterSet(filters))

  def load[K: SupportedKey: ClassTag](layerId: LayerId, filters: FilterSet[K]): Try[RasterRDD[K]] =
    metaDataCatalog.load(layerId).flatMap { case (metaData, params) =>
      load(metaData, params, filters)
    }

  def load[K: SupportedKey: ClassTag](metaData: LayerMetaData, params: Params, filters: FilterSet[K]): Try[RasterRDD[K]]

  def save[K: SupportedKey: ClassTag](layerId: LayerId, rdd: RasterRDD[K]): Try[Unit] =
    save(layerId, rdd, false)

  def save[K: SupportedKey: ClassTag](layerId: LayerId, rdd: RasterRDD[K], clobber: Boolean): Try[Unit] =
    save(layerId, rdd, paramsFor(layerId), clobber)

  def save[K: SupportedKey: ClassTag](layerId: LayerId, rdd: RasterRDD[K], params: Params): Try[Unit] =
    save(layerId, rdd, params, false)

  def save[K: SupportedKey: ClassTag](layerId: LayerId, rdd: RasterRDD[K], params: Params, clobber: Boolean): Try[Unit] = {
    val metaData = LayerMetaData(layerId, rdd.metaData)
    metaDataCatalog.save(metaData, params, clobber)
    save(rdd, metaData, params, clobber)
  }

  // protected so metaDataCatalog is always updated before this is called
  protected def save[K: SupportedKey: ClassTag](rdd: RasterRDD[K], layerMetaData: LayerMetaData, params: Params, clobber: Boolean): Try[Unit]
}
