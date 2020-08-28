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
import geotrellis.geowave.conf.StoreConfiguration
import geotrellis.geowave.dsl.json.{JsonValidator, _}
import io.circe.generic.extras.ConfiguredJsonCodec
import org.locationtech.geowave.core.store.api.{DataStore, Index}

/** Kafka message to configure and create an index for specific data type
  *
  * @param indices    List of index definitions that would be used by an adapter
  * @param typeName   Name of the type to be used in the table
  * @param dataType   Data type to expect for ingest
  * @param namespace  Store (i.e. Cassandra) namespace
  */
@ConfiguredJsonCodec
case class IndexParameters(
  /** A list of [[IndexDefinition]]s
    * Each index corresponds to a table in which each record will be stored
    */
  indices: List[IndexDefinition],

  /** Adapter name with configured indexes
    * This is going to be used as a name for [[geotrellis.geowave.adapter.GeoTrellisDataAdapter]]
    */
  typeName: TypeName,

  /** Name of data type supported by the Adapter
    * This is used to produce an adapter instance that will be registered with given index
    */
  dataType: DataType,

  /** Storage (i.e. Cassandra) namespace to be used for connection
    * Follow up ingest message should be using consistent namespace values
    */
  namespace: Option[String] = None
) extends DataAdapterParameters {
  def dataStore: DataStore = StoreConfiguration.getDataStore(namespace)
}

object IndexParameters {
  implicit val indexParametersValidator: JsonValidator[IndexParameters] = { json =>
    JsonValidator
      .validateIndexParameters(json)
      .toEither
      .flatMap(_ => json.as[IndexParameters].leftMap(JsonValidatorErrors(_)))
  }
}

/** Each adapter can have a relation to multiple indicies.
  * This class defines a single Index definition
  *
  * @param indexName    Unique name for index instance
  *                     The instance captures index type (ex: spatial, spatial_temporal) and the parameters, dimension resolutions and or bounds
  *                     This name will be auto-generated based on index type and options but may be overwritten here
  *
  *                     The index name is used as table name for all ingested data.
  *                     Using this ...
  * @param indexType    Name of the index type available through GeoWave IndexPlugin SPI
  * @param indexOptions Index Options specific to each [[indexType]] within GeoWave IndexPlugin SPI
  *                      These are options may be configured through:
  *                        'org.locationtech.geowave.core.store.spi.DimensionalityTypeProviderSpi'
  */
@ConfiguredJsonCodec
case class IndexDefinition(
  indexName: Option[String],
  indexType: IndexType,
  indexOptions: Map[String, String]
) {
  def getIndex: Index = indexType.getIndex(indexOptions, indexName)
}

object IndexDefinition {
  implicit def indexDefinitiontoList(indexDefinition: IndexDefinition): List[IndexDefinition] = indexDefinition :: Nil
}