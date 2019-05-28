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
import geotrellis.spark.util.KryoWrapper
import geotrellis.util.annotations._
import geotrellis.vector._

import org.apache.avro.Schema
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.opengis.feature.simple._
import mil.nga.giat.geowave.datastore.accumulo.operations.config._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.core.store.query._
import mil.nga.giat.geowave.mapreduce.input._
import mil.nga.giat.geowave.core.store.spi._
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.datastore.accumulo.operations.config._
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.store.operations.remote.options._

import scala.reflect._


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoWaveFeatureRDDReader {

  /**
    * $experimental Read out an RDD of Vector features from an
    * accumulo geowave store
    *
    * @param zookeepers            zookeeper master node location
    * @param accumuloInstanceName  name of the accumulo instance to connect to
    * @param accumuloInstanceUser  user under whose authority accumulo actions should be carried out
    * @param accumuloInstancePass  password matching the provided user
    * @param gwNamespace           the geowave namespace for this data
    * @param simpleFeatureType     the GeoTools [[SimpleFeature]] specification
    * @param query                 the geowave query to use in reading from accumulo
    * @param queryLimit            the maximum number of returned results (0 is infinite)
    *
    * @tparam G                    the type of geometry to be retrieved through geowave (REQUIRED)
    * @note                        If the above type parameter is not supplied, errors WILL be thrown
    */
  @experimental def read[G <: Geometry : ClassTag](
    zookeepers: String,
    accumuloInstanceName: String,
    accumuloInstanceUser: String,
    accumuloInstancePass: String,
    gwNamespace: String,
    simpleFeatureType: SimpleFeatureType,
    query: DistributableQuery = new BasicQuery(new BasicQuery.Constraints()),
    queryLimit: Int = 0
  )(implicit sc: SparkContext): RDD[Feature[G, Map[String, Object]]] = {
    val hadoopConf = sc.hadoopConfiguration
    val jobConf = Job.getInstance(hadoopConf).getConfiguration

    val additionalAccumuloOpts = new AccumuloOptions
    additionalAccumuloOpts.setUseAltIndex(true)

    val accumuloOpts = new AccumuloRequiredOptions
    accumuloOpts.setZookeeper(zookeepers)
    accumuloOpts.setInstance(accumuloInstanceName)
    accumuloOpts.setUser(accumuloInstanceUser)
    accumuloOpts.setPassword(accumuloInstancePass)
    accumuloOpts.setGeowaveNamespace(gwNamespace)
    accumuloOpts.setAdditionalOptions(additionalAccumuloOpts)

    val pluginOpts = new DataStorePluginOptions
    pluginOpts.selectPlugin("accumulo")
    pluginOpts.setFactoryOptions(accumuloOpts)

    val gwDataAdapter = new FeatureDataAdapter(simpleFeatureType)
    val gw2dIndex = (new SpatialDimensionalityTypeProvider).createPrimaryIndex
    val queryOptions = new QueryOptions(gwDataAdapter, gw2dIndex)
    queryOptions.setLimit(queryLimit)

    GeoWaveInputFormat.setStoreOptions(jobConf, pluginOpts)
    GeoWaveInputFormat.setQuery(jobConf, query)
    GeoWaveInputFormat.setQueryOptions(jobConf, queryOptions)

    val simpleFeatureRDD = sc.newAPIHadoopRDD(
      jobConf,
      classOf[GeoWaveInputFormat[SimpleFeature]],
      classOf[GeoWaveInputKey],
      classOf[SimpleFeature]
    )

    simpleFeatureRDD.map({ case (gwInputKey, simpleFeature) =>
      simpleFeature.toFeature[G]()
    })

  }
}
