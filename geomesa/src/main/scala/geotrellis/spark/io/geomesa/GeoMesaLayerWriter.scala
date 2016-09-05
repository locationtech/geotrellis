package geotrellis.spark.io.geomesa

import geotrellis.spark._
import geotrellis.vector._
import geotrellis.geotools._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.geotools.feature.DefaultFeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

class GeoMesaLayerWriter(val attributeStore: GeoMesaAttributeStore, table: String)(implicit sc: SparkContext) extends Serializable {
  def write[G <: Geometry, D: ? => Seq[(String, Any)]]
    (layerId: LayerId, rdd: RDD[Feature[G, D]])
    (implicit ev: Feature[G, D] => FeatureToGeoMesaSimpleFeatureMethods[G, D]): Unit = {

    val fi = attributeStore.layerIdString(layerId)
    rdd
      .zipWithIndex // it is necessary to provide unique id for ingest batch
      .map { case (f, index) => val sf = f.toSimpleFeature(s"${fi}_${index}"); sf.getFeatureType -> sf }.groupByKey
      .foreachPartition { (partition: Iterator[(SimpleFeatureType, Iterable[SimpleFeature])]) =>
      // data store per partition
      val dataStore = attributeStore.getAccumuloDataStore(table)
      // register feature types and fill feature collections
      partition.foreach { case (sft, sf) =>
        val featureCollection = new DefaultFeatureCollection
        dataStore.createSchema(sft)
        sf.foreach(featureCollection.add)
        dataStore.getFeatureSource(sft.getTypeName).addFeatures(featureCollection)
      }

      dataStore.dispose
    }
  }
}
