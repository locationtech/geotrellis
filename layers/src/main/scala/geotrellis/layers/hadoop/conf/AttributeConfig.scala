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

package geotrellis.layers.hadoop.conf

import geotrellis.util.CamelCaseConfig

import pureconfig.generic.auto._


case class AttributeCachingConfig(
  expirationMinutes: Int = 60,
  maxSize: Int = 1000,
  enabled: Boolean = true
)

case class AttributeConfig(caching: AttributeCachingConfig = AttributeCachingConfig())

object AttributeConfig extends CamelCaseConfig {
  lazy val conf: AttributeConfig = pureconfig.loadConfigOrThrow[AttributeConfig]("geotrellis.attribute")
  implicit def attributeConfigToClass(obj: AttributeConfig.type): AttributeConfig = conf
}
