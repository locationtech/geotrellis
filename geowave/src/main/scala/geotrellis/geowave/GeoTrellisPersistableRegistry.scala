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

package geotrellis.geowave

import geotrellis.geowave.adapter.geotiff.GeoTiffAdapter
import geotrellis.geowave.adapter.raster.MulitbandRasterAdapter
import geotrellis.geowave.index.dimension.{Elevation, ElevationDefinition}
import geotrellis.geowave.index.field.ElevationField
import org.locationtech.geowave.core.index.persist.PersistableRegistrySpi
import org.locationtech.geowave.core.index.persist.PersistableRegistrySpi.PersistableIdAndConstructor

/**
 * To be sure that we didn't forget to register classes it is possible to use the following test class from the GeoWave tests:
 * https://github.com/locationtech/geowave/blob/v1.0.0/core/store/src/test/java/org/locationtech/geowave/core/store/TestStorePersistableRegistry.java
 *
 * GeoWave has a complete test example of creating custom index with custom dimensions.
 */
class GeoTrellisPersistableRegistry extends PersistableRegistrySpi {
  protected val INITIAL_ID_HANDLERS: Short   = 4000
  protected val INITIAL_ID_ADAPTERS: Short   = 5000
  protected val INITIAL_ID_DIMENSIONS: Short = 6000
  protected val INITIAL_ID_FIELDS: Short     = 7000
  protected val INITIAL_ID_DEFINITION: Short = 8000

  def getSupportedPersistables: Array[PersistableRegistrySpi.PersistableIdAndConstructor] =
    Array(
      /** Handlers */
      new PersistableIdAndConstructor(INITIAL_ID_HANDLERS, () => new GeoTiffAdapter.GeometryHandler()),
      new PersistableIdAndConstructor((INITIAL_ID_HANDLERS + 1).toShort, () => new MulitbandRasterAdapter.GeometryHandler()),
      new PersistableIdAndConstructor((INITIAL_ID_HANDLERS + 2).toShort, () => new GeoTiffAdapter.TimestampHandler()),
      new PersistableIdAndConstructor((INITIAL_ID_HANDLERS + 3).toShort, () => new GeoTiffAdapter.ElevationHandler()),

      /** Adapters */
      new PersistableIdAndConstructor(INITIAL_ID_ADAPTERS, () => new GeoTiffAdapter()),
      new PersistableIdAndConstructor((INITIAL_ID_ADAPTERS + 1).toShort, () => new MulitbandRasterAdapter()),

      /** Index dimensions */
      new PersistableIdAndConstructor(INITIAL_ID_DIMENSIONS, () => new Elevation()),

      /** Index dimension fields */
      new PersistableIdAndConstructor(INITIAL_ID_FIELDS, () => new ElevationField()),

      /** Index dimension definition */
      new PersistableIdAndConstructor(INITIAL_ID_DEFINITION, () => new ElevationDefinition(0, 320000))
    )
}
