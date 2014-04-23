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

package geotrellis

import com.typesafe.config.{ConfigFactory,Config}

case class GeoTrellisConfig(catalogPath:Option[String])

object GeoTrellisConfig {
  def apply():GeoTrellisConfig = apply(ConfigFactory.load())

  def apply(config:Config):GeoTrellisConfig = {
    val catalogPath = if(config.hasPath("geotrellis.catalog")) {
      Some(config.getString("geotrellis.catalog"))
    } else { None }
    GeoTrellisConfig(catalogPath)
  }

  def apply(catalogPath:String):GeoTrellisConfig =
    GeoTrellisConfig(Some(catalogPath))
}
