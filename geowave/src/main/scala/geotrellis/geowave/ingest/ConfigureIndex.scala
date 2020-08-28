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

package geotrellis.geowave.ingest

import cats.data.Validated.Invalid
import cats.effect.Sync
import geotrellis.geowave.adapter.GeoTrellisDataAdapter
import geotrellis.geowave.dsl._
import org.locationtech.geowave.core.store.api.Index
import cats.data.Validated.Valid

object ConfigureIndex {
  private final val logger = org.slf4j.LoggerFactory.getLogger(this.getClass())

  def apply(params: IndexParameters): List[Index] = {
    val adapter = GeoTrellisDataAdapter.load(params.dataType, params.typeName)
    val indices = params.indices.map { indexDefinition =>
      val index = indexDefinition.getIndex
      adapter.validateIndexCompatability(index)
        .leftMap{ msg =>
          s"'${indexDefinition.indexType}' index type is incompatable with '${adapter.getTypeName}' adapter: $msg"
        }
    }
    val errors = indices.collect { case Invalid(e) => e }
    if (errors.nonEmpty) throw new IllegalArgumentException(errors.mkString("; "))
    val validIndeces = indices.collect { case Valid(i) => i}

    // and persist them; so the name is valid for adapter type + a list of indices
    params.dataStore.addType(adapter, validIndeces: _*)
    logger.info(s"Created index: ${validIndeces.map(_.getName)}")
    validIndeces
  }

  def applyF[F[_]: Sync](params: IndexParameters): F[List[Index]] = Sync[F].delay(apply(params))
}
