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

package geotrellis.vector

import org.locationtech.jts.{geom => jts}

sealed trait Dimensions {
  private[vector] val jtsGeom: jts.Geometry
}

trait AtLeastOneDimension extends Dimensions
trait AtMostOneDimension extends Dimensions

trait ZeroDimensions extends Dimensions
                        with AtMostOneDimension

trait OneDimension extends Dimensions
                       with AtMostOneDimension
                       with AtLeastOneDimension

trait TwoDimensions extends Dimensions
                       with AtLeastOneDimension
