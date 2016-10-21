package geotrellis.spark.io.geomesa

import geotrellis.geotools._
import geotrellis.spark._
import geotrellis.vector._

import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.geotools.data._
import org.locationtech.geomesa.jobs.mapreduce.GeoMesaInputFormat
import org.locationtech.geomesa.jobs.GeoMesaConfigurator
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.reflect.ClassTag

class GeoMesaFeatureReader(val instance: GeoMesaInstance)(implicit sc: SparkContext) extends Serializable {
  def readSimpleFeatures(
    featureName: String,
    simpleFeatureType: SimpleFeatureType,
    query: Query,
    numPartitions: Option[Int] = None
  ): RDD[SimpleFeature] = {
    val dataStore = instance.accumuloDataStore
    try {
      if (!dataStore.getTypeNames().contains(simpleFeatureType.getTypeName)) dataStore.createSchema(simpleFeatureType)
    } finally dataStore.dispose()

    val job = Job.getInstance(sc.hadoopConfiguration)
    GeoMesaInputFormat.configure(job, instance.conf, query)

    if (numPartitions.isDefined) {
      GeoMesaConfigurator.setDesiredSplits(job.getConfiguration, numPartitions.get * sc.getExecutorStorageStatus.length)
      InputFormatBase.setAutoAdjustRanges(job, false)
    }

    sc.newAPIHadoopRDD(job.getConfiguration, classOf[GeoMesaInputFormat], classOf[Text], classOf[SimpleFeature]).map(U => U._2)
  }

  def read[G <: geotrellis.vector.Geometry: ClassTag, D]
    (layerId: LayerId, simpleFeatureType: SimpleFeatureType, query: Query, numPartitions: Option[Int] = None)
    (implicit transmute: Map[String, Any] => D): RDD[Feature[G, D]] =
      readSimpleFeatures(
        featureName       = layerId.name,
        simpleFeatureType = simpleFeatureType,
        query             = query,
        numPartitions     = numPartitions
      ).map(_.toFeature[G, D]())
}

object GeoMesaFeatureReader {
  def apply(instance: GeoMesaInstance)(implicit sc: SparkContext): GeoMesaFeatureReader = new GeoMesaFeatureReader(instance)
}
