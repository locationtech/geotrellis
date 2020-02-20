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

trait Dimension[G <: Geometry]

trait AtLeastOneDimension[G <: Geometry] extends Dimension[G]
trait AtMostOneDimension[G <: Geometry] extends Dimension[G]

trait ZeroDimensional[G <: Geometry] extends Dimension[G]
                         with AtMostOneDimension[G]

trait OneDimensional[G <: Geometry] extends Dimension[G]
                        with AtMostOneDimension[G]
                        with AtLeastOneDimension[G]

trait TwoDimensional[G <: Geometry] extends Dimension[G]
                        with AtLeastOneDimension[G]
