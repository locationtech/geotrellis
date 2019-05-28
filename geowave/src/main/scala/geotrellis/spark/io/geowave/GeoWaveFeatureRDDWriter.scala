/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.geowave

import geotrellis.geotools._
import geotrellis.spark._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.layers.io.index._
import geotrellis.spark.util.KryoWrapper
import geotrellis.util.annotations.experimental
import geotrellis.vector._

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


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoWaveFeatureRDDWriter {

  /**
    * $experimental Read out an RDD of Vector features from an
    * accumulo geowave store
    *
    * @param features              an RDD of [[geotrellis.vector.Feature]] objects to be written
    * @param zookeepers            zookeeper master node location
    * @param accumuloInstanceName  name of the accumulo instance to connect to
    * @param accumuloInstanceUser  user under whose authority accumulo actions should be carried out
    * @param accumuloInstancePass  password matching the provided user
    * @param gwNamespace           the geowave namespace for this data
    * @param simpleFeatureType     the GeoTools [[SimpleFeature]] specification which corresponds to
    *                               all supplied features
    *
    * @tparam G                    the type of geometry to be retrieved through geowave (REQUIRED)
    */
  @experimental def write[G <: Geometry, D](
    features: RDD[Feature[G, D]],
    zookeepers: String,
    accumuloInstanceName: String,
    accumuloInstanceUser: String,
    accumuloInstancePass: String,
    gwNamespace: String,
    simpleFeatureType: SimpleFeatureType
  )(implicit transmute: D => Seq[(String, Any)]): Unit = {
    implicit val sc = features.sparkContext
    val trans = KryoWrapper(transmute)
    val kryoFeatureType = KryoWrapper(simpleFeatureType)
    features.foreachPartition({ featureIterator =>
      // Secure the basic operations
      val accumuloOperations =
        new BasicAccumuloOperations(
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
      val gwDataStore =
        new AccumuloDataStore(
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

      val writer =
        gwDataStore
          .createWriter(gwDataAdapter, gw2dIndex)
          .asInstanceOf[IndexWriter[SimpleFeature]]

      featureIterator.foreach({ feature =>
        writer.write(feature.toSimpleFeature())
      })
      writer.close()
    })
  }
}
