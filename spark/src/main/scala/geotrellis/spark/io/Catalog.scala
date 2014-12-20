package geotrellis.spark.io

import geotrellis.spark._

import scala.reflect.ClassTag

object Catalog {
  object AttributeKeys {
    val rasterMetaData = "raster-metadata"
  }
}

trait RasterCatalog {
  type SupportedKey[K]

  def load[K: SupportedKey: ClassTag](layerId: LayerId, metaData: RasterMetaData): RasterRDD[K]
  def save[K: SupportedKey: ClassTag](layerId: LayerId, rdd: RasterRDD[K]): Unit
}

trait Catalog {
  type Params
  type SupportedKey[K]

  def attributeCatalog: AttributeCatalog

  def paramsFor[K: SupportedKey: ClassTag](layerId: LayerId): Params

  def load[K: SupportedKey: ClassTag](layerId: LayerId): RasterRDD[K] =
    load(layerId, paramsFor[K](layerId), new FilterSet[K])

  def load[K: SupportedKey: ClassTag](layerId: LayerId, params: Params): RasterRDD[K] =
    load(layerId, params, new FilterSet[K])

  def load[K: SupportedKey: ClassTag](layerId: LayerId, filters: FilterSet[K]): RasterRDD[K] = {
    val metaData = attributeCatalog.load[RasterMetaData](layerId, Catalog.AttributeKeys.rasterMetaData)
    load(layerId, metaData.rasterMetaData, paramsFor(layerId), filters)
  }

  def load[K: SupportedKey: ClassTag](layerId: LayerId, params: Params, filters: FilterSet[K]): RasterRDD[K] = {
    val metaData = attributeCatalog.load[RasterMetaData](layerId, Catalog.AttributeKeys.rasterMetaData)
    load(layerId, metaData.rasterMetaData, params, filters)
  }

  def load[K: SupportedKey: ClassTag](layerId: LayerId, metaData: RasterMetaData, params: Params, filters: FilterSet[K]): RasterRDD[K]

  def save[K: SupportedKey: ClassTag](id: LayerId, rdd: RasterRDD[K]): Unit =
    save(id, rdd, false)

  def save[K: SupportedKey: ClassTag](id: LayerId, rdd: RasterRDD[K], clobber: Boolean): Unit =
    save(id, paramsFor(id), rdd, clobber)

  def save[K: SupportedKey: ClassTag](id: LayerId, params: Params, rdd: RasterRDD[K]): Unit =
    save(id, params, rdd, false)

  /** The implementer of this method is responsible for saving the metaData to the metaDataCatalog */
  def save[K: SupportedKey: ClassTag](id: LayerId, params: Params, rdd: RasterRDD[K], clobber: Boolean): Unit
}
