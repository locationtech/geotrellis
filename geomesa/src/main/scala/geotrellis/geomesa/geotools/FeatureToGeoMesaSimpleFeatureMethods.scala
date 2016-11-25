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

package geotrellis.geomesa.geotools

import geotrellis.proj4.CRS
import geotrellis.util.annotations.experimental
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Feature, Geometry}

import org.opengis.feature.simple.SimpleFeature

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental trait FeatureToGeoMesaSimpleFeatureMethods[G <: Geometry, T] extends MethodExtensions[Feature[G, T]] {

  /** $experimental */
  @experimental def toSimpleFeature(featureName: String, featureId: Option[String] = None, crs: Option[CRS] = None)(implicit transmute: T => Seq[(String, Any)]): SimpleFeature =
    GeometryToGeoMesaSimpleFeature(featureName, self.geom, featureId, crs, self.data)
}
