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

import cats.instances.option._
import cats.syntax.either._
import cats.syntax.semigroup._
import geotrellis.geowave.adapter.TypeName
import geotrellis.geowave.api._
import geotrellis.geowave.dsl.json._
import geotrellis.geowave.index.dimension.ElevationDefinition
import io.circe.generic.extras.ConfiguredJsonCodec
import geotrellis.vector._
import org.locationtech.geowave.core.geotime.index.dimension.{SimpleTimeDefinition, TimeDefinition}
import org.locationtech.geowave.core.geotime.store.dimension.Time.TimeRange
import org.locationtech.geowave.core.geotime.util.GeometryUtils
import org.locationtech.geowave.core.index.sfc.data.NumericRange
import org.locationtech.geowave.core.store.query.constraints.BasicQueryByClass.{ConstraintData, ConstraintSet, ConstraintsByClass}
import org.locationtech.geowave.core.store.query.filter.BasicQueryFilter

@ConfiguredJsonCodec
case class DeleteParameters(
  typeName: TypeName,
  indexName: String,
  geometry: Option[Geometry] = None,
  namespace: Option[String] = None,
  time: Option[TimeRange] = None,
  elevation: Option[NumericRange] = None,
  compareOp: BasicQueryFilter.BasicQueryCompareOperation = BasicQueryFilter.BasicQueryCompareOperation.INTERSECTS
) extends QueryConfiguration {
  def constraints: Option[ConstraintsByClass] = {
    val gc = geometry.map(GeometryUtils.basicConstraintsFromGeometry)
    val tc = time.map { timeRange =>
      new ConstraintsByClass(
        new ConstraintSet(
          new ConstraintData(timeRange.toNumericData(), false),
          classOf[TimeDefinition],
          classOf[SimpleTimeDefinition]
        )
      )
    }
    val dc = elevation.map { elevationRange =>
      new ConstraintsByClass(
        new ConstraintSet(
          new ConstraintData(elevationRange, false),
          classOf[ElevationDefinition]
        )
      )
    }

    (gc |+| tc) |+| dc
  }
}

object DeleteParameters {
  implicit val deleteParametersValidator: JsonValidator[DeleteParameters] = { json =>
    JsonValidator
      .validateDeleteParameters(json)
      .toEither
      .flatMap(_ => json.as[DeleteParameters].leftMap(JsonValidatorErrors(_)))
  }
}
