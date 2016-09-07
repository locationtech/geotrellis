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

class GeoMesaLayerReader(val attributeStore: GeoMesaAttributeStore, table: String)(implicit sc: SparkContext) extends Serializable {
  lazy val dsConf = attributeStore.getConf(table, raw = true).asInstanceOf[Map[String, String]]

  def readSimpleFeatures[G <: geotrellis.vector.Geometry: ClassTag]
    (featureName: String, query: Query, simpleFeatureType: SimpleFeatureType, numPartitions: Option[Int] = None): RDD[SimpleFeature] = {
    val dataStore = attributeStore.getAccumuloDataStore(table)
    dataStore.createSchema(simpleFeatureType)
    dataStore.dispose()

    val job = Job.getInstance(sc.hadoopConfiguration)
    GeoMesaInputFormat.configure(job, dsConf, query)

    if (numPartitions.isDefined) {
      GeoMesaConfigurator.setDesiredSplits(job.getConfiguration, numPartitions.get * sc.getExecutorStorageStatus.length)
      InputFormatBase.setAutoAdjustRanges(job, false)
    }

    sc.newAPIHadoopRDD(job.getConfiguration, classOf[GeoMesaInputFormat], classOf[Text], classOf[SimpleFeature]).map(U => U._2)
  }

  def read[G <: geotrellis.vector.Geometry: ClassTag, D]
    (layerId: LayerId, query: Query, simpleFeatureType: SimpleFeatureType, numPartitions: Option[Int] = None)
    (implicit transmute: Map[String, Any] => D): RDD[Feature[G, D]] =
      readSimpleFeatures[G](
        featureName       = layerId.name,
        query             = query,
        simpleFeatureType = simpleFeatureType,
        numPartitions     = numPartitions
      ).map(_.toFeature[G, D]())
}

