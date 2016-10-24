package geotrellis.spark.io.geomesa

import geotrellis.geomesa.geotools._
import geotrellis.spark._
import geotrellis.util.annotations.experimental
import geotrellis.vector._

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.geotools.data.Transaction
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeoMesaFeatureWriter(val instance: GeoMesaInstance)
  (implicit sc: SparkContext) extends Serializable with LazyLogging {

  logger.error("GeoMesa support is experimental")

  /** $experimental */
  @experimental def write[G <: Geometry, D: ? => Seq[(String, Any)]]
    (layerId: LayerId, rdd: RDD[Feature[G, D]])
    (implicit ev: Feature[G, D] => FeatureToGeoMesaSimpleFeatureMethods[G, D]): Unit = {

    // SimpleFeatureType requires valid UnmodifiableCollection kryo serializer
    rdd
      .map { f => val sf = f.toSimpleFeature(layerId.name); sf.getFeatureType -> sf }.groupByKey
      .foreachPartition { (partition: Iterator[(SimpleFeatureType, Iterable[SimpleFeature])]) =>
        // data store per partition
        val dataStore = instance.accumuloDataStore
        partition.foreach { case (sft, sf) =>
          // register feature types and write features
          dataStore.createSchema(sft)
          val featureWriter = dataStore.getFeatureWriterAppend(sft.getTypeName, Transaction.AUTO_COMMIT)
          try {
            sf.foreach { rawFeature =>
              val newFeature = featureWriter.next()
              (0 until sft.getAttributeCount).foreach(i => newFeature.setAttribute(i, rawFeature.getAttribute(i)))
              featureWriter.write()
            }
          } finally featureWriter.close()
        }
        dataStore.dispose()
      }
  }
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoMesaFeatureWriter {

  /** $experimental */
  @experimental def apply(instance: GeoMesaInstance)(implicit sc: SparkContext): GeoMesaFeatureWriter =
    new GeoMesaFeatureWriter(instance)
}
