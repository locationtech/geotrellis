/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.io.avro

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder.FieldAssembler

import scala.collection.JavaConverters._

package object codecs {
  private[codecs] def injectFields(from: Schema, to: FieldAssembler[Schema]): FieldAssembler[Schema] = {
    from.getFields.asScala.foldLeft(to){
      case (builder, field) ⇒
        builder.name(field.name()).`type`(field.schema()).noDefault()
    }
  }
}
