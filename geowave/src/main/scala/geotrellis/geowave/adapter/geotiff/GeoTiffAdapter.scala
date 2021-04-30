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

package geotrellis.geowave.adapter.geotiff

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

import geotrellis.geowave.GeoTrellisPersistableRegistry
import geotrellis.geowave.adapter._
import geotrellis.geowave.dsl.syntax._
import geotrellis.geowave.index.dimension.Elevation
import geotrellis.raster.MultibandTile
import geotrellis.raster.io.geotiff.GeoTiff
import org.locationtech.geowave.core.geotime.store.dimension.{GeometryWrapper, Time}
import org.locationtech.geowave.core.store.data.field.{FieldReader, FieldWriter}

/**
 * All adapters should have an empty constructor to use Serialization mechanisms.
 * Each new Adapter should also be registered in the [[GeoTrellisPersistableRegistry]].
 */
class GeoTiffAdapter(
  private var typeName: TypeName = "".typeName,
  private var fieldHandlers: List[IndexFieldHandler[GeoTiff[MultibandTile]]] = List(
    new GeoTiffAdapter.GeometryHandler,
    new GeoTiffAdapter.TimestampHandler,
    new GeoTiffAdapter.ElevationHandler)
) extends GeoTrellisDataAdapter[GeoTiff[MultibandTile]](typeName, fieldHandlers) {
  protected val DATA_FIELD_ID = "GT_TIFF"

  /**
   * For SimpleFeatures it is a FeatureId, for model this can be a ModelId, smth to help filter the results
   * In case the partitionKey would mach across several entries due to SFC limitations
   */
  def getDataId(t: GeoTiff[MultibandTile]): Array[Byte] = Array(0)

  /** Gets a reader to read the value from the row */
  def getReader(fieldName: String): FieldReader[AnyRef] =
    if(fieldName == DATA_FIELD_ID) new GeoTiffFieldReader() else null

  /** Gets a writer to write the value into the row */
  def getWriter(fieldName: String): FieldWriter[GeoTiff[MultibandTile], AnyRef] =
    new GeoTiffFieldWriter()
}

object GeoTiffAdapter {

  /** A function that extracts [[geotrellis.vector.Extent]] from the [[GeoTiff[MultibandTile]]] */
  class GeometryHandler extends GeometryFieldHandler[GeoTiff[MultibandTile]] {
    def toIndexValue(tiff: GeoTiff[MultibandTile]): GeometryWrapper =
      new GeometryWrapper(tiff.extent.toPolygon(), Array())
  }


  /** A function that extracts [[Elevation]] from the [[GeoTiff[MultibandTile]]] */
  class ElevationHandler extends ElevationFieldHandler[GeoTiff[MultibandTile]] {
    def toIndexValue(tiff: GeoTiff[MultibandTile]): Elevation =
      Elevation(java.lang.Double.parseDouble(tiff.tags.headTags("ELEVATION")))
  }


  /** A function that extracts [[Time]] from the [[GeoTiff[MultibandTile]]] */
  class TimestampHandler extends TimestampFieldHandler[GeoTiff[MultibandTile]] {
    def toIndexValue(tiff: GeoTiff[MultibandTile]): Time = {
      val dateString = tiff.tags.headTags(GEOTIFF_TIME_TAG_DEFAULT)
      val dateTime = ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString))
      new Time.Timestamp(dateTime.toInstant.toEpochMilli, Array())
    }
  }

  /** Corresponds to Time.DEFAULT_FIELD_ID */
  val GEOTIFF_TIME_TAG_DEFAULT       = "TIFFTAG_DATETIME"
  val GEOTIFF_TIME_FORMAT_DEFAULT    = "yyyy:MM:dd HH:mm:ss"
  val GEOTIFF_TIME_FORMAT            = "GEOTIFF_TIME_FORMAT"
  val GEOTIFF_TIME_FORMATTER_DEFAULT = timeFormatter(GEOTIFF_TIME_FORMAT_DEFAULT)

  def timeFormatter(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC)
}
