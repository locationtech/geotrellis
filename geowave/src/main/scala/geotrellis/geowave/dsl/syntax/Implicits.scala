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

package geotrellis.geowave.dsl.syntax

import geotrellis.geowave.adapter._
import geotrellis.geowave.dsl._

trait Implicits {
  implicit class NewtypeOps(val self: String) {
    def indexType: IndexType           = IndexType(self)
    def typeName: TypeName             = TypeName(self)
    def indexFieldName: IndexFieldName = IndexFieldName(self)
    def dataType: DataType             = DataType(self)
    def dataStoreType: DataStoreType   = DataStoreType(self)
  }
}

object Implicits extends Implicits
