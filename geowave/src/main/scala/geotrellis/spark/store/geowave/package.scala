/*
 * Copyright 2019 Azavea
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

package geotrellis.spark.store

import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.io.WKTWriter
import com.vividsolutions.jts.io.{WKTReader => OLDWKTReader}
import com.vividsolutions.jts.{geom => jtsOld}

package object geowave {
  /** An ugly conversion function from the new jts.Geometry type into the old GeoWave compatible Geometry type */
  implicit def geometryConversion(geom: jts.Geometry): jtsOld.Geometry =
    new OLDWKTReader().read(new WKTWriter().write(geom))
}
