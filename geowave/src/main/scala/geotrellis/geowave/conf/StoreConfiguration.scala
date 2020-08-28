/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.conf

import cats.effect.Sync
import cats.syntax.option._
import geotrellis.geowave.dsl.DataStoreType
import org.locationtech.geowave.core.store.api.DataStore
import pureconfig._
import pureconfig.generic.auto._

case class StoreConfiguration(
  dataStoreType: DataStoreType,
  options: Option[Map[String, String]] = None
) {
  def getDataStore: DataStore = dataStoreType.getDataStore(options.getOrElse(Map()))
  def getDataStore(namespace: String): DataStore = getDataStore(namespace.some)
  def getDataStore(namespace: Option[String]): DataStore = {
    val opts = options.getOrElse(Map())
    dataStoreType.getDataStore(namespace.fold(opts)(ns => opts + ("gwNamespace" -> ns)))
  }
}

object StoreConfiguration {
  lazy val load: StoreConfiguration = ConfigSource.default.at("geotrellis.geowave.connection.store").loadOrThrow[StoreConfiguration]

  implicit def StoreConfigurationToClass(obj: StoreConfiguration.type): StoreConfiguration = load
}
