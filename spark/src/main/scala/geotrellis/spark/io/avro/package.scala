/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.io

import geotrellis.util._
import org.apache.avro._
import org.apache.avro.generic._

package object avro {
  implicit class withGenericRecordMethods(val self: GenericRecord) extends MethodExtensions[GenericRecord] {
    def apply[X](name: String) = self.get(name).asInstanceOf[X]
  }

  implicit class withSchemaMethods(val self: Schema) extends MethodExtensions[Schema] {
    def fingerprintMatches(other: Schema): Boolean =
      SchemaNormalization.parsingFingerprint64(self) == SchemaNormalization.parsingFingerprint64(other)
  }
}
