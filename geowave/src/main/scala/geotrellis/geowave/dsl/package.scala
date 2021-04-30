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

package geotrellis.geowave

import geotrellis.geowave.utils._
import io.estatico.newtype.macros.newsubtype
import org.locationtech.geowave.core.cli.prefix.PrefixedJCommander
import org.locationtech.geowave.core.store.api.{DataStore, Index}
import org.locationtech.geowave.core.store.cli.store.DataStorePluginOptions
import org.locationtech.geowave.core.store.index.IndexPluginOptions

package object dsl {

  /** Name of index type registered with GeoWave IndexPlugin SPI */
  @newsubtype case class IndexType(value: String) {
    def getIndex(options: Map[String, String], indexName: Option[String] = None): Index = {
      val outputIndexOptions: IndexPluginOptions = new IndexPluginOptions
      outputIndexOptions.selectPlugin(value)

      if(options.nonEmpty) {
        val opts = outputIndexOptions.getDimensionalityOptions
        val commander = new PrefixedJCommander()
        commander.addPrefixedObject(opts)
        commander.parse(options.prefixedList: _*)
      }

      indexName.foreach(outputIndexOptions.setName)
      outputIndexOptions.createIndex()
    }
  }

  @newsubtype case class DataStoreType(value: String) {
    def getDataStore(options: Map[String, String]): DataStore = {
      val dataStorePluginOptions = new DataStorePluginOptions()
      dataStorePluginOptions.selectPlugin(value)

      if(options.nonEmpty) {
        val opts = dataStorePluginOptions.getFactoryOptions
        val commander = new PrefixedJCommander()
        commander.addPrefixedObject(opts)
        commander.parse(options.prefixedList: _*)
      }

      dataStorePluginOptions.createDataStore()
    }
  }
}
