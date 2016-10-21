package geotrellis.spark.io.geomesa

import geotrellis.spark._
import geotrellis.vector._
import geotrellis.geomesa.geotools._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.geotools.data.Transaction
import org.opengis.feature.simple.SimpleFeatureType

class GeoMesaFeatureWriter(val instance: GeoMesaInstance)(implicit sc: SparkContext) extends Serializable {
  def write[G <: Geometry, D: ? => Seq[(String, Any)]: λ[α => Feature[G, α] => FeatureToGeoMesaSimpleFeatureMethods[G, α]]]
    (layerId: LayerId, simpleFeatureType: SimpleFeatureType, rdd: RDD[Feature[G, D]]): Unit = {

    val (sftTypeName, sftAttributeCount) = simpleFeatureType.getTypeName -> simpleFeatureType.getAttributeCount
    // data store on a driver
    val dataStore = instance.accumuloDataStore
    try {
      // register feature type and write features
      if (!dataStore.getTypeNames().contains(sftTypeName)) dataStore.createSchema(simpleFeatureType)
    } finally dataStore.dispose()

    rdd
      .map(_.toSimpleFeature(layerId.name))
      .foreachPartition { partition =>
        // data store per partition
        val dataStore = instance.accumuloDataStore

        // writer per partition
        val featureWriter = dataStore.getFeatureWriterAppend(sftTypeName, Transaction.AUTO_COMMIT)
        try {
          partition.foreach { sf =>
            val newFeature = featureWriter.next()
            (0 until sftAttributeCount).foreach(i => newFeature.setAttribute(i, sf.getAttribute(i)))
            featureWriter.write()
          }
        } finally {
          featureWriter.close(); dataStore.dispose()
        }
      }
  }
}

object GeoMesaFeatureWriter {
  def apply(instance: GeoMesaInstance)(implicit sc: SparkContext): GeoMesaFeatureWriter =
    new GeoMesaFeatureWriter(instance)
}
