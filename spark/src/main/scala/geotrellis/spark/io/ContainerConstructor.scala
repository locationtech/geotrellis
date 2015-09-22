package geotrellis.spark.io

import geotrellis.spark._
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

/**
 * This TypeClass abstracts away the specific RDD container, like `RasterRDD` and `MultiBandRasterRDD` allowing them 
 * to be used generically generally by readers and writers.
 * 
 * We abstract over the metadata that is held in the container as it can be handled existentially.
 * 
 * @tparam K Type of RDD Key (ex: SpatialKey)
 * @tparam V Type of RDD Value (ex: Tile)
 * @tparam C RDD Container (ex: RasterRDD)
 */
trait ContainerConstructor[K, V, C[_]] {
  type MetaDataType

  implicit def metaDataFormat: JsonFormat[MetaDataType]

  def getMetaData(container: C[K]): MetaDataType
  def makeContainer(rdd: RDD[(K, V)], bounds: KeyBounds[K], metadata: MetaDataType): C[K]
}



