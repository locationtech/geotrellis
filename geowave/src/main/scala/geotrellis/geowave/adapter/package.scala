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

import io.estatico.newtype.macros.newsubtype
import org.locationtech.geowave.core.store.data.field.{FieldReader, FieldWriter}

package object adapter {

  /** Input data type that can be used to find a [[GeoTrellisDataAdapter]] instance.
    * DataType describes both the format and semantics of the data
    *
    * DataType describes how to get the reader for the data we need to ingest.
    * It does not describe how it will be stored and indexed, that is controled by the DataAdapter.
    */
  @newsubtype case class DataType(value: String)

  /** Name for [[GeoTrellisDataAdapter]] instance.
    * - This name is provided during index configuration
    * - This name must be given when ingesting a file
    */
  @newsubtype case class TypeName(value: String)

  /** Name of a field on which a feature is indexed.
    *
    * List of field names per DataType are defined by which [[NamedIndexFieldHandler]]s are  provided to [[GeoTrellisDataAdapter]].
    * Thus this name only has any meaning in relation to specific [[GeoTrellisDataAdapter]] instance.
    * Index field values are stored in Common Index Data and are used for query refinement.
    */
  @newsubtype case class IndexFieldName(value: String)

  implicit def upcastFieldReader[T](fieldReader: FieldReader[T]): FieldReader[AnyRef] = fieldReader.asInstanceOf[FieldReader[AnyRef]]
  implicit def upcastFieldWriter[T](fieldWriter: FieldWriter[T, T]): FieldWriter[T, AnyRef] = fieldWriter.asInstanceOf[FieldWriter[T, AnyRef]]
}
