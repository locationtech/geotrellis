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

package geotrellis.geowave.index.query

import java.util.Date

import geotrellis.geowave.index.dimension.ElevationDefinition
import org.locationtech.geowave.core.geotime.index.dimension.{SimpleTimeDefinition, TimeDefinition}
import org.locationtech.geowave.core.geotime.store.query.ExplicitSpatialQuery
import org.locationtech.geowave.core.geotime.util.GeometryUtils
import org.locationtech.geowave.core.index.sfc.data.NumericRange
import org.locationtech.geowave.core.store.query.constraints.BasicQueryByClass.{ConstraintData, ConstraintSet, ConstraintsByClass}
import org.locationtech.jts.geom.Geometry

class ExplicitSpatialTemporalElevationQuery(constraints: ConstraintsByClass, geometry: Geometry) extends ExplicitSpatialQuery(constraints, geometry)

object ExplicitSpatialTemporalElevationQuery {
  def apply(depth: Double, startTime: Date, endTime: Date, queryGeometry: Geometry): ExplicitSpatialTemporalElevationQuery =
    apply(depth, depth, startTime, endTime, queryGeometry)

  def apply(minElevation: Double, maxElevation: Double, minTime: Date, maxTime: Date, queryGeometry: Geometry): ExplicitSpatialTemporalElevationQuery = {
    val geoConstraints = GeometryUtils.basicConstraintsFromGeometry(queryGeometry)
    val temporalConstraints = new ConstraintsByClass(
      new ConstraintSet(
        new ConstraintData(new NumericRange(minTime.getTime.toDouble, maxTime.getTime.toDouble), false),
        classOf[TimeDefinition],
        classOf[SimpleTimeDefinition]
      )
    )
    val depthConstraints = new ConstraintsByClass(
      new ConstraintSet(
        new ConstraintData(new NumericRange(minElevation, maxElevation), false),
        classOf[ElevationDefinition]
      )
    )

    new ExplicitSpatialTemporalElevationQuery(geoConstraints.merge(temporalConstraints).merge(depthConstraints), queryGeometry)
  }
}
