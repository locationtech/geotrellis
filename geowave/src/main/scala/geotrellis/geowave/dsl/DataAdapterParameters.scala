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

package geotrellis.geowave.dsl

import cats.syntax.either._
import geotrellis.geowave.adapter.{DataType, TypeName}
import geotrellis.geowave.dsl.json.{JsonValidator, _}
import io.circe.generic.extras.ConfiguredJsonCodec

trait DataAdapterParameters {
  /** Adapter name with configured indexes
    * This is going to be used as a name for [[geotrellis.geowave.adapter.GeoTrellisDataAdapter]]
    */
  def typeName: TypeName

  /** Name of data type supported by the Adapter
    * This is used to produce an adapter instance that will be registered with given index
    */
  def dataType: DataType

  /** Storage (i.e. Cassandra) namespace to be used for connection
    * Follow up ingest message should be using consistent namespace values
    */
  def namespace: Option[String]
}

object DataAdapterParameters {
  @ConfiguredJsonCodec
  case class Header(typeName: TypeName, dataType: DataType, namespace: Option[String]) extends DataAdapterParameters

  implicit val ingestParametersValidator: JsonValidator[DataAdapterParameters.Header] = { json =>
    JsonValidator
      .validateMessageHeader(json)
      .toEither
      .flatMap(_ => json.as[DataAdapterParameters.Header].leftMap(JsonValidatorErrors(_)))
  }
}
