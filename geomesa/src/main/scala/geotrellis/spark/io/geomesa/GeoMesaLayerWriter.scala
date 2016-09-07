package geotrellis.spark.io.geomesa

import geotrellis.spark._
import geotrellis.vector._
import geotrellis.geotools._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.geotools.data.Transaction
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.collection.JavaConversions._

class GeoMesaLayerWriter(val attributeStore: GeoMesaAttributeStore, table: String)(implicit sc: SparkContext) extends Serializable {
  def write[G <: Geometry, D: ? => Seq[(String, Any)]]
    (layerId: LayerId, rdd: RDD[Feature[G, D]])
    (implicit ev: Feature[G, D] => FeatureToGeoMesaSimpleFeatureMethods[G, D]): Unit = {

    rdd
      .zipWithIndex // it is necessary to provide unique id for ingest batch
      .map { case (f, index) => val sf = f.toSimpleFeature(s"${attributeStore.layerIdString(layerId)}_${index}", layerId.name); sf.getFeatureType -> sf }.groupByKey
      .foreachPartition { (partition: Iterator[(SimpleFeatureType, Iterable[SimpleFeature])]) =>
        // data store per partition
        val dataStore = attributeStore.getAccumuloDataStore(table)
        partition.foreach { case (sft, sf) =>
          // register feature types and write features
          dataStore.createSchema(sft)
          val featureWriter = dataStore.getFeatureWriterAppend(sft.getTypeName, Transaction.AUTO_COMMIT)
          val attrNames = featureWriter.getFeatureType.getAttributeDescriptors.map(_.getLocalName)
          try {
            sf.foreach { rawFeature =>
              val newFeature = featureWriter.next()
              attrNames.foreach(an => newFeature.setAttribute(an, rawFeature.getAttribute(an)))
              featureWriter.write()
            }
          } finally featureWriter.close()
        }
        dataStore.dispose()
      }
  }
}
