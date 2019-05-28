/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io.accumulo.conf

import geotrellis.layers.io.hadoop.conf.CamelCaseConfig
import geotrellis.spark.util.threadsFromString
import pureconfig.generic.auto._

case class AccumuloCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class AccumuloRDDConfig(write: String = "default") {
  def writeThreads: Int = threadsFromString(write)
}

case class AccumuloThreadsConfig(
  collection: AccumuloCollectionConfig = AccumuloCollectionConfig(),
  rdd: AccumuloRDDConfig = AccumuloRDDConfig()
)

case class AccumuloConfig(
  catalog: String = "metadata",
  threads: AccumuloThreadsConfig = AccumuloThreadsConfig()
)

object AccumuloConfig extends CamelCaseConfig {
  lazy val conf: AccumuloConfig = pureconfig.loadConfigOrThrow[AccumuloConfig]("geotrellis.accumulo")
  implicit def accumuloConfigToClass(obj: AccumuloConfig.type): AccumuloConfig = conf
}
