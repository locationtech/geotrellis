/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster.io.geotiff.reader.utils.ByteBufferUtils._

import java.nio.ByteBuffer

import monocle.syntax._
import monocle.macros.Lenses

import geotrellis.raster.io.geotiff.reader.Tags._
import geotrellis.raster.io.geotiff.reader.GeoKeys._

case class GeoKeyReader(byteBuffer: ByteBuffer,
  directory: ImageDirectory) {

  def read(geoKeyDirectory: GeoKeyDirectory, index: Int = 0):
      GeoKeyDirectory = index match {
    case geoKeyDirectory.count => geoKeyDirectory
    case _ => {
      val keyEntryMetadata = GeoKeyMetadata(
        byteBuffer.getUnsignedShort,
        byteBuffer.getUnsignedShort,
        byteBuffer.getUnsignedShort,
        byteBuffer.getUnsignedShort
      )

      val updatedDirectory = readGeoKeyEntry(keyEntryMetadata, geoKeyDirectory)

      read(updatedDirectory, index + 1)
    }
  }

  private def readGeoKeyEntry(keyMetadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory)  = keyMetadata.tiffTagLocation match {
    case 0 => readShort(keyMetadata, geoKeyDirectory)
    case DoublesTag => readDoubles(keyMetadata, geoKeyDirectory)
    case AsciisTag => readAsciis(keyMetadata, geoKeyDirectory)
  }

  private def readShort(keyMetadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val short = keyMetadata.valueOffset

    keyMetadata.keyID match {
      case GTModelTypeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._configKeys ^|->
        ConfigKeys._gtModelType set(short)
      case GTRasterTypeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._configKeys ^|->
        ConfigKeys._gtRasterType set(Some(short))
      case GeogTypeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogType set(Some(short))
      case GeogGeodeticDatumGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogGeodeticDatum set(Some(short))
      case GeogPrimeMeridianGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogPrimeMeridian set(Some(short))
      case GeogLinearUnitsGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogLinearUnits set(Some(short))
      case GeogAngularUnitsGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogAngularUnits set(Some(short))
      case GeogEllipsoidGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogEllipsoid set(Some(short))
      case GeogAzimuthUnitsGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogAzimuthUnits set(Some(short))
      case ProjectedCSTypeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedCSType set(short)
      case ProjectionGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projection set(Some(short))
      case ProjCoordTransGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projCoordTrans set(Some(short))
      case ProjLinearUnitsGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projLinearUnits set(Some(short))
      case VerticalCSTypeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._verticalCSKeys ^|->
        VerticalCSKeys._verticalCSType set(Some(short))
      case VerticalDatumGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._verticalCSKeys ^|->
        VerticalCSKeys._verticalDatum set(Some(short))
      case VerticalUnitsGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._verticalCSKeys ^|->
        VerticalCSKeys._verticalUnits set(Some(short))
      case tag => geoKeyDirectory &|->
        GeoKeyDirectory._nonStandardizedKeys ^|->
        NonStandardizedKeys._shortMap modify (
          _ + (tag -> short)
        )
    }
  }

  private def readDoubles(keyMetadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val doubles = directory
      .geoTiffTags
      .doubles
      .get
      .drop(keyMetadata.valueOffset)
      .take(keyMetadata.count)

    keyMetadata.keyID match {
      case GeogLinearUnitSizeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogLinearUnitSize set(Some(doubles(0)))
      case GeogAngularUnitSizeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogAngularUnitSize set(Some(doubles(0)))
      case GeogSemiMajorAxisGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogSemiMajorAxis set(Some(doubles(0)))
      case GeogSemiMinorAxisGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogSemiMinorAxis set(Some(doubles(0)))
      case GeogInvFlatteningGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogInvFlattening set(Some(doubles(0)))
      case GeogPrimeMeridianLongGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogPrimeMeridianLong set(Some(doubles(0)))
      case ProjLinearUnitSizeGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projLinearUnitSize set(Some(doubles(0)))
      case ProjStdParallel1GeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projStdParallel1 set(Some(doubles(0)))
      case ProjStdParallel2GeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projStdParallel2 set(Some(doubles(0)))
      case ProjNatOriginLongGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projNatOriginLong set(Some(doubles(0)))
      case ProjNatOriginLatGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projNatOriginLat set(Some(doubles(0)))
      case ProjFalseEastingGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedFalsings ^|->
        ProjectedFalsings._projFalseEasting set(Some(doubles(0)))
      case ProjFalseNorthingGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedFalsings ^|->
        ProjectedFalsings._projFalseNorthing set(Some(doubles(0)))
      case ProjFalseOriginLongGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedFalsings ^|->
        ProjectedFalsings._projFalseOriginLong set(Some(doubles(0)))
      case ProjFalseOriginLatGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedFalsings ^|->
        ProjectedFalsings._projFalseOriginLat set(Some(doubles(0)))
      case ProjFalseOriginEastingGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedFalsings ^|->
        ProjectedFalsings._projFalseOriginEasting set(Some(doubles(0)))
      case ProjFalseOriginNorthingGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projectedFalsings ^|->
        ProjectedFalsings._projFalseOriginNorthing set(Some(doubles(0)))
      case ProjCenterLongGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projCenterLong set(Some(doubles(0)))
      case ProjCenterLatGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projCenterLat set(Some(doubles(0)))
      case ProjCenterEastingGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projCenterEasting set(Some(doubles(0)))
      case ProjCenterNorthingGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projCenterNorthing set(Some(doubles(0)))
      case ProjScaleAtNatOriginGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projScaleAtNatOrigin set(Some(doubles(0)))
      case ProjScaleAtCenterGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projScaleAtCenter set(Some(doubles(0)))
      case ProjAzimuthAngleGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projAzimuthAngle set(Some(doubles(0)))
      case ProjStraightVertPoleLongGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projStraightVertPoleLong set(Some(doubles(0)))
      case ProjRectifiedGridAngleGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._projRectifiedGridAngle set(Some(doubles(0)))
      case tag => geoKeyDirectory &|->
        GeoKeyDirectory._nonStandardizedKeys ^|->
        NonStandardizedKeys._doublesMap modify (
          _ + (tag -> doubles)
        )
    }
  }

  private def readAsciis(metadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val strings = directory
      .geoTiffTags
      .asciis
      .get
      .substring(metadata.valueOffset, metadata.count + metadata.valueOffset)
      .split("\\|")
      .toArray

    metadata.keyID match {
      case GTCitationGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._configKeys ^|->
        ConfigKeys._gtCitation set(Some(strings))
      case GeogCitationGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._geogCSParameterKeys ^|->
        GeogCSParameterKeys._geogCitation set(Some(strings))
      case PCSCitationGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._projectedCSParameterKeys ^|->
        ProjectedCSParameterKeys._pcsCitation set(Some(strings))
      case VerticalCitationGeoKey => geoKeyDirectory &|->
        GeoKeyDirectory._verticalCSKeys ^|->
        VerticalCSKeys._verticalCitation set(Some(strings))
      case tag => geoKeyDirectory &|->
        GeoKeyDirectory._nonStandardizedKeys ^|->
        NonStandardizedKeys._asciisMap modify (
          _ + (tag -> strings)
        )
    }
  }

}
