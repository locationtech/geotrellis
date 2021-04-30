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

package geotrellis.geowave.index

import com.beust.jcommander.IStringConverter
import geotrellis.geowave.index.dimension.ElevationDefinition
import geotrellis.geowave.index.field.ElevationField
import org.geotools.referencing.CRS
import org.locationtech.geowave.core.geotime.index.dimension.TemporalBinningStrategy.Unit
import org.locationtech.geowave.core.geotime.index.dimension.{LatitudeDefinition, LongitudeDefinition, TimeDefinition}
import org.locationtech.geowave.core.geotime.store.dimension._
import org.locationtech.geowave.core.geotime.util.GeometryUtils
import org.locationtech.geowave.core.index.dimension.NumericDimensionDefinition
import org.locationtech.geowave.core.index.sfc.SFCFactory.SFCType
import org.locationtech.geowave.core.index.sfc.xz.XZHierarchicalIndexFactory
import org.locationtech.geowave.core.store.api.Index
import org.locationtech.geowave.core.store.dimension.NumericDimensionField
import org.locationtech.geowave.core.store.index.{BasicIndexModel, CustomNameIndex}
import org.locationtech.geowave.core.store.spi.DimensionalityTypeProviderSpi
import org.opengis.referencing.crs.CoordinateReferenceSystem
import spire.syntax.cfor._

class SpatialTemporalElevationIndexTypeProvider extends DimensionalityTypeProviderSpi[SpatialTemporalElevationOptions] {
  import SpatialTemporalElevationIndexTypeProvider._

  private val DEFAULT_SP_TEMP_ELV_ID_STR = "SP_TEMP_ELV_IDX"

  def getDimensions(options: SpatialTemporalElevationOptions): Array[NumericDimensionDefinition] =
    Array(
      new LongitudeDefinition(),
      new LatitudeDefinition(true),
      new TimeDefinition(Unit.YEAR),
      new ElevationDefinition(minValue=0, maxValue=options.getMaxElevation)
    )

  def getSpatialTemporalFields(geometryPrecision: Int): Array[NumericDimensionField[_]] =
    Array(
      new LongitudeField(geometryPrecision),
      new LatitudeField(geometryPrecision, true),
      new TimeField(Unit.YEAR),
      new ElevationField()
    )

  def getDimensionalityTypeName: String = "spatial_temporal_elevation"

  def getDimensionalityTypeDescription: String = "This dimensionality type matches all indices that only require Geometry, Time and Elevation."

  /**
   * This is a modified copy of org.locationtech.geowave.core.geotime.ingest.SpatialTemporalDimensionalityTypeProvider::internalCreateIndex:
   * https://github.com/locationtech/geowave/blob/v1.0.0/core/geotime/src/main/java/org/locationtech/geowave/core/geotime/ingest/SpatialTemporalDimensionalityTypeProvider.java#L94
   */
  def createIndex(options: SpatialTemporalElevationOptions): Index = {
    val geometryPrecision: Int = options.getGeometryPrecision()

    val (dimensions, fields, isDefaultCRS, crsCode) =
      if ((options.getCrs == null) || options.getCrs.isEmpty || options.getCrs.equalsIgnoreCase(GeometryUtils.DEFAULT_CRS_STR)) {
        (getDimensions(options), getSpatialTemporalFields(geometryPrecision), true, "EPSG:4326")
      } else {
        val crs          = decodeCRS(options.getCrs)
        val cs           = crs.getCoordinateSystem
        val isDefaultCRS = false
        val crsCode      = options.getCrs
        val dimensions   = Array.ofDim[NumericDimensionDefinition](cs.getDimension + 2)
        val fields       = Array.ofDim[NumericDimensionField[_]](dimensions.length)

        cfor(0)(_ < dimensions.length - 2, _ + 1) { d =>
          val csa = cs.getAxis(d)
          dimensions(d) = new CustomCRSBoundedSpatialDimension(d.toByte, csa.getMinimumValue, csa.getMaximumValue)
          fields(d)     = new CustomCRSSpatialField(dimensions(d).asInstanceOf[CustomCRSBoundedSpatialDimension], geometryPrecision)
        }

        val elevationDefinition = new ElevationDefinition(minValue = 0, maxValue = options.getMaxElevation)
        dimensions(dimensions.length - 2) = new TimeDefinition(options.getTemporalPerioidicty)
        fields(dimensions.length - 2)     = new TimeField(options.getTemporalPerioidicty)
        dimensions(dimensions.length - 1) = elevationDefinition
        fields(dimensions.length - 1)     = new ElevationField(baseDefinition = elevationDefinition)

        (dimensions, fields, isDefaultCRS, crsCode)
      }

    val indexModel: BasicIndexModel =
      if (isDefaultCRS) new BasicIndexModel(fields) else new CustomCrsIndexModel(fields, crsCode)

    // index combinedId
    val combinedId: String =
      if (isDefaultCRS) s"${DEFAULT_SP_TEMP_ELV_ID_STR}_${options.getBias}_${options.getTemporalPerioidicty}_${options.getMaxElevation}"
      else s"${DEFAULT_SP_TEMP_ELV_ID_STR}_${crsCode.substring(crsCode.indexOf(":") + 1)}_${options.getBias}_${options.getTemporalPerioidicty}_${options.getMaxElevation}"

    new CustomNameIndex(
      XZHierarchicalIndexFactory.createFullIncrementalTieredStrategy(
        dimensions,
        Array[Int](
          options.getBias.getSpatialPrecision,
          options.getBias.getSpatialPrecision,
          options.getBias.getTemporalPrecision,
          options.getBias.getSpatialPrecision
        ),
        SFCType.HILBERT,
        options.getMaxDuplicates
      ),
      indexModel,
      combinedId
    )
  }

  def createOptions: SpatialTemporalElevationOptions = new SpatialTemporalElevationOptions()
}

object SpatialTemporalElevationIndexTypeProvider {
  class IntConverter extends IStringConverter[Int] {
    def convert(value: String): Int = java.lang.Integer.parseUnsignedInt(value)
  }

  def decodeCRS(crsCode: String): CoordinateReferenceSystem = CRS.decode(crsCode, true)
}
