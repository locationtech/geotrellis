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

package geotrellis.store.s3.conf

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import software.amazon.awssdk.services.s3.model.RequestPayer

case class S3Config(requestPayer: Option[RequestPayer] = None)

object S3Config {
  implicit val requestPayerReader: ConfigReader[RequestPayer] = ConfigReader[String].map(RequestPayer.fromValue)

  lazy val conf: S3Config = ConfigSource.default.at("geotrellis.s3").loadOrThrow[S3Config]
  implicit def s3ConfigToClass(obj: S3Config.type): S3Config = conf
}
