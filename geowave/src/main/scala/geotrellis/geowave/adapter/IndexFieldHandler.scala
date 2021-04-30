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

package geotrellis.geowave.adapter

import geotrellis.geowave.dsl.syntax._
import org.locationtech.geowave.core.index.StringUtils
import org.locationtech.geowave.core.index.persist.Persistable
import org.locationtech.geowave.core.store.index.CommonIndexValue

/** This class is used by the DataAdapter to translate between native values and persistence
  * encoded values. The basic implementation of this will perform type matching on the index field type.
  *
  * The field name is mutable so it can be made to match field names required by a given index.
  */
trait IndexFieldHandler[RowType] extends Persistable {
  protected var fieldName: IndexFieldName

  def getFieldName: IndexFieldName = fieldName

  def setFieldName(name: IndexFieldName): Unit = this.fieldName = name

  def toIndexValue(row: RowType): CommonIndexValue

  override def toBinary: Array[Byte] = {
    StringUtils.stringToBinary(fieldName)
  }

  override def fromBinary(bytes: Array[Byte]): Unit = {
    fieldName = StringUtils.stringFromBinary(bytes).indexFieldName
  }
}
