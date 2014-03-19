/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.process.json

import com.typesafe.config._
import collection.JavaConversions._

import scala.collection.mutable

// example json is available in the geotrellis.process.catalog tests. please
// keep it up-to-date with changes you make here.

object CatalogParser {
  def error(msg:String) = sys.error(s"Invalid json in catalog: $msg")

  def apply(jsonString:String,catalogPath:String):CatalogRec = {
    val json = ConfigFactory.parseString(jsonString)

    val catalog =
      try {
        json.getString("catalog")
      } catch {
        case _:ConfigException.Missing =>
          error("Must have 'catalog' property with catalog name.")
        case _:ConfigException.WrongType =>
          error("'catalog' property must be a string.")
      }
    if(catalog == "") error("Catalog must have a name field 'catalog' be non-empty")

    val storesList = 
      try {
        json.getConfigList("stores")
      } catch {
        case _:ConfigException.Missing =>
          error("Must have 'stores' property with list of data stores.")
        case _:ConfigException.WrongType =>
          error("'stores' property must be a list of data stores.")
      }

    val stores = storesList.map(parseDataStore(catalogPath,_)).toList

    CatalogRec(catalog,stores)
  }

  private def parseDataStore(catalogPath:String,storeConfig:Config):DataStoreRec = {
    val store =
      try {
        storeConfig.getString("store")
      } catch {
        case _:ConfigException.Missing =>
          error("Data store must have 'store' property with data store name.")
        case _:ConfigException.WrongType =>
          error("'store' property must be a string.")
      }

    val paramsConfig = 
      try {
        storeConfig.getConfig("params")
      } catch {
        case _:ConfigException.Missing =>
          error("Data store must have 'params' property with parameters.")
        case _:ConfigException.WrongType =>
          error("'param' property must be a json object.")
      }

    val params = 
      paramsConfig.root.keys.map { key =>
        val value =
          try {
            paramsConfig.getString(key)
          } catch {
            case _:ConfigException.WrongType =>
              error("'param' property must be a json object.")
          }
        (key,value)
      }

    DataStoreRec(store, params.toMap,catalogPath)
  }
}
