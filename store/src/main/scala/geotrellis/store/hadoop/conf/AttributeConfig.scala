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

package geotrellis.store.hadoop.conf

import pureconfig.{ConfigReader, ConfigSource}

case class AttributeCachingConfig(
  expirationMinutes: Int = 60,
  maxSize: Int = 1000,
  enabled: Boolean = true
)

case class AttributeConfig(caching: AttributeCachingConfig = AttributeCachingConfig())

object AttributeCachingConfig {
  implicit val attributeCachingConfigReader: ConfigReader[AttributeCachingConfig] =
    ConfigReader.forProduct3[AttributeCachingConfig, Option[Int], Option[Int], Option[Boolean]](
      "expiration-minutes", "max-size", "enabled"
    ) { (expirationMinutes, maxSize, enabled) =>
      AttributeCachingConfig(expirationMinutes.getOrElse(60), maxSize.getOrElse(1000), enabled.getOrElse(true))
    }
}

object AttributeConfig {
  implicit val attributeConfigReader: ConfigReader[AttributeConfig] =
    ConfigReader.forProduct1[AttributeConfig, Option[AttributeCachingConfig]]("caching") { caching =>
      AttributeConfig(caching.getOrElse(AttributeCachingConfig()))
    }

  lazy val conf: AttributeConfig = ConfigSource.default.at("geotrellis.attribute").loadOrThrow[AttributeConfig]
  implicit def attributeConfigToClass(obj: AttributeConfig.type): AttributeConfig = conf
}
