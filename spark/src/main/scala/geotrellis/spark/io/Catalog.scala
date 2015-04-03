package geotrellis.spark.io

import geotrellis.spark._

import scala.reflect.ClassTag

trait Catalog {
  type Params
  type SupportedKey[K]

  def metaDataCatalog: MetaDataCatalog[Params]

  def paramsFor[K: SupportedKey: ClassTag](layerId: LayerId): Params

  def load[K: SupportedKey: ClassTag](id: LayerId): RasterRDD[K] =
    load(id, new FilterSet[K])

  def load[K: SupportedKey: ClassTag](id: LayerId, filters: FilterSet[K]): RasterRDD[K] = {
    val (metaData, params) = metaDataCatalog.load(id)
    load(id, metaData.rasterMetaData, params, filters)
  }

  def load[K: SupportedKey: ClassTag](id: LayerId, metaData: RasterMetaData, params: Params, filters: FilterSet[K]): RasterRDD[K]

  def save[K: SupportedKey: ClassTag](id: LayerId, rdd: RasterRDD[K]): Unit =
    save(id, rdd, false)

  def save[K: SupportedKey: ClassTag](id: LayerId, rdd: RasterRDD[K], clobber: Boolean): Unit =
    save(id, paramsFor(id), rdd, clobber)

  def save[K: SupportedKey: ClassTag](id: LayerId, params: Params, rdd: RasterRDD[K]): Unit =
    save(id, params, rdd, false)

  /** The implementer of this method is responsible for saving the metaData to the metaDataCatalog */
  def save[K: SupportedKey: ClassTag](id: LayerId, params: Params, rdd: RasterRDD[K], clobber: Boolean): Unit
}
