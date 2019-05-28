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

package geotrellis.spark.io.geomesa

import geotrellis.geotools._
import geotrellis.spark._
import geotrellis.util.annotations.experimental
import geotrellis.vector._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.layers.LayerId
import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.geotools.data._
import org.locationtech.geomesa.jobs.interop.mapreduce.GeoMesaAccumuloInputFormat
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeoMesaFeatureReader(val instance: GeoMesaInstance)
                                        (implicit sc: SparkContext) extends Serializable with LazyLogging {

  logger.error("GeoMesa support is experimental")

  /** $experimental */
  @experimental def readSimpleFeatures(
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
    GeoMesaAccumuloInputFormat.configure(job, instance.conf.asJava, query)

    if (numPartitions.isDefined) {
      System.setProperty("geomesa.mapreduce.splits.max", numPartitions.get.toString)
      InputFormatBase.setAutoAdjustRanges(job, false)
    }
    sc.newAPIHadoopRDD(job.getConfiguration, classOf[GeoMesaAccumuloInputFormat], classOf[Text], classOf[SimpleFeature]).map(U => U._2)
  }

  /** $experimental */
  @experimental def read[G <: geotrellis.vector.Geometry: ClassTag, D]
    (layerId: LayerId, simpleFeatureType: SimpleFeatureType, query: Query, numPartitions: Option[Int] = None)
    (implicit transmute: Map[String, Any] => D): RDD[Feature[G, D]] =
      readSimpleFeatures(
        featureName       = layerId.name,
        simpleFeatureType = simpleFeatureType,
        query             = query,
        numPartitions     = numPartitions
      ).map(_.toFeature[G, D]())
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoMesaFeatureReader {

  /** $experimental */
  @experimental def apply(instance: GeoMesaInstance)(implicit sc: SparkContext): GeoMesaFeatureReader = new GeoMesaFeatureReader(instance)
}
