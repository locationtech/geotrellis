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

package geotrellis.geowave.adapter.raster

import geotrellis.geowave.GeoTrellisPersistableRegistry
import geotrellis.geowave.adapter._
import geotrellis.geowave.dsl.syntax._
import geotrellis.raster.{MultibandTile, Raster}
import org.locationtech.geowave.core.geotime.store.dimension.GeometryWrapper
import org.locationtech.geowave.core.store.data.field.{FieldReader, FieldWriter}

/**
 * All adapters should have an empty constructor to use Serialization mechanisms.
 * Each new Adapter should also be registered in the [[GeoTrellisPersistableRegistry]].
 */
class MulitbandRasterAdapter(
  private var typeName: TypeName = "".typeName,
  private var fieldHandlers: List[IndexFieldHandler[Raster[MultibandTile]]] = List(
    new MulitbandRasterAdapter.GeometryHandler
  )
) extends GeoTrellisDataAdapter[Raster[MultibandTile]](typeName, fieldHandlers) {
  protected val DATA_FIELD_ID = "GT_RASTER"

  /**
   * For SimpleFeatures it is a FeatureId, for model this can be a ModelId, smth to help filter the results
   * In case the partitionKey would mach across several entries due to SFC limitations
   */
  def getDataId(t: Raster[MultibandTile]): Array[Byte] = Array(0)

  /** Gets a reader to read the value from the row */
  def getReader(fieldName: String): FieldReader[AnyRef] = if(fieldName == DATA_FIELD_ID) new MultibandRasterReader() else null

  /** Gets a writer to write the value into the row */
  def getWriter(fieldName: String): FieldWriter[Raster[MultibandTile], AnyRef] = new MultibandRasterWriter()
}

object MulitbandRasterAdapter {
  /** A function that extracts [[geotrellis.vector.Extent]] from the [[Raster[MultibandTile]]] */
  class GeometryHandler extends GeometryFieldHandler[Raster[MultibandTile]] {
    def toIndexValue(raster: Raster[MultibandTile]): GeometryWrapper =
      new GeometryWrapper(raster.extent.toPolygon(), Array())
  }
}
