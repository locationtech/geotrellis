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

package geotrellis.raster


/**
  * The base class for the two concrete [[PixelSampleType]]s.
  */
abstract sealed class PixelSampleType

/**
  * Type encoding the fact that pixels should be treated as points.
  */
case object PixelIsPoint extends PixelSampleType

/**
  * Type encoding the fact that pixels should be treated as
  * suitably-tiny extents.
  */
case object PixelIsArea extends PixelSampleType
