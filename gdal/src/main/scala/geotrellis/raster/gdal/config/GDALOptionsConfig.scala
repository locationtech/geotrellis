/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.gdal.config

import com.azavea.gdal.GDALWarp
import geotrellis.raster.gdal.GDALDataset
import geotrellis.raster.gdal.GDALDataset.DatasetType

import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.collection.concurrent.TrieMap

case class GDALOptionsConfig(options: Map[String, String] = Map.empty, acceptableDatasets: List[String] = List("SOURCE", "WARPED"), numberOfAttempts: Int = 1 << 20) {
  def set: Unit = {
    // register first config options from the conf file
    options.foreach { case (key, value) => GDALWarp.set_config_option(key, value) }
    // register programmatically set options
    GDALOptionsConfig.setRegistryOptions
  }

  def getAcceptableDatasets: Set[DatasetType] = {
    val res = acceptableDatasets.collect {
      case "SOURCE" => GDALDataset.SOURCE
      case "WARPED" => GDALDataset.WARPED
    }

    if(res.nonEmpty) res.toSet else Set(GDALDataset.SOURCE, GDALDataset.WARPED)
  }

  def getNumberOfAttempts: Int = if(numberOfAttempts > 0) numberOfAttempts else 1 << 20
}

object GDALOptionsConfig extends Serializable {
  private val optionsRegistry = TrieMap[String, String]()

  def registerOption(key: String, value: String): Unit = optionsRegistry += (key -> value)
  def registerOptions(seq: (String, String)*): Unit = seq.foreach(optionsRegistry += _)
  def setRegistryOptions: Unit = optionsRegistry.foreach { case (key, value) => GDALWarp.set_config_option(key, value) }
  def setOptions: Unit = { conf.set; setRegistryOptions }

  lazy val conf: GDALOptionsConfig = ConfigSource.default.at("geotrellis.raster.gdal").loadOrThrow[GDALOptionsConfig]
  implicit def gdalOptionsConfig(obj: GDALOptionsConfig.type): GDALOptionsConfig = conf
}
