/*
 * Copyright 2017 Astraea, Inc.
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

package geotrellis.proj4

import geotrellis.proj4.CRS.ObjectNameToString
import org.osgeo.proj4j.CoordinateReferenceSystem

/**
 * Sinusoidal projection, commonly used with MODIS-based data products.
 */
object Sinusoidal extends CRS with ObjectNameToString {
  lazy val proj4jCrs: CoordinateReferenceSystem = factory.createFromParameters(null,
    "+proj=sinu +lon_0=0 +x_0=0 +y_0=0 +a=6371007.181 +b=6371007.181 +units=m +no_defs")
  val epsgCode: Option[Int] = None
}
