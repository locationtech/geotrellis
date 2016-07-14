package geotrellis.spark.io.geowave

import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.util.KryoWrapper
import geotrellis.vector._
import geotrellis.geotools._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.opengis.feature.simple._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.mapreduce.input._
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.core.store.spi._
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.datastore.accumulo.operations.config._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.adapter.vector._

import scala.reflect._

object GeoWaveFeatureRDDWriter {

  def write[G <: Geometry, D](
    features: RDD[Feature[G, D]],
    zookeepers: String,
    accumuloInstanceName: String,
    accumuloInstanceUser: String,
    accumuloInstancePass: String,
    gwNamespace: String,
    simpleFeatureType: SimpleFeatureType
  )(implicit sc: SparkContext, transmute: D => Seq[(String, Any)]): Unit = {
    implicit val sc = features.sparkContext
    val kryoFeatureType = KryoWrapper(simpleFeatureType)
    features.foreach({ feature =>
      // Secure the basic operations
      val accumuloOperations = new BasicAccumuloOperations(
        zookeepers,
        accumuloInstanceName,
        accumuloInstanceUser,
        accumuloInstancePass,
        gwNamespace
      )

      // Generate accumulo options instance
      val accumuloOpts = new AccumuloOptions
      accumuloOpts.setPersistDataStatistics(true)

      // Initialize geowave datastore
      val gwDataStore = new AccumuloDataStore(
        new AccumuloIndexStore(accumuloOperations),
        new AccumuloAdapterStore(accumuloOperations),
        new AccumuloDataStatisticsStore(accumuloOperations),
        new AccumuloSecondaryIndexDataStore(accumuloOperations),
        new AccumuloAdapterIndexMappingStore(accumuloOperations),
        accumuloOperations,
        accumuloOpts
      )

      val gwDataAdapter = new FeatureDataAdapter(kryoFeatureType.value)
      val gw2dIndex = (new SpatialDimensionalityTypeProvider).createPrimaryIndex

      val writer = gwDataStore.createWriter(gwDataAdapter, gw2dIndex).asInstanceOf[IndexWriter[SimpleFeature]]
      writer.write(feature.toSimpleFeature())
      writer.close()
    })

  }
}
