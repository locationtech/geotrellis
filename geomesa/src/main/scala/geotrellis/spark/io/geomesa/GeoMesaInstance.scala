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

import geotrellis.util.annotations.experimental
import com.typesafe.scalalogging.LazyLogging
import geotrellis.layers.LayerId
import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import scala.collection.JavaConverters._

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeoMesaInstance(val conf: Map[String, String])
    extends Serializable with LazyLogging {
  logger.error("GeoMesa support is experimental")

  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def dataStore = DataStoreFinder.getDataStore(conf.asJava)
  def accumuloDataStore = dataStore.asInstanceOf[AccumuloDataStore]
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoMesaInstance {

  /** $experimental */
  @experimental def apply(tableName: String, instanceName: String, zookeepers: String, user: String, password: String, mock: Boolean = false): GeoMesaInstance = {
    new GeoMesaInstance(Map(
      "accumulo.instance.id" -> instanceName,
      "accumulo.zookeepers"  -> zookeepers,
      "accumulo.user"        -> user,
      "accumulo.password"    -> password,
      "accumulo.catalog"     -> tableName,
      "accumulo.mock"        -> mock.toString
    ))
  }
}
