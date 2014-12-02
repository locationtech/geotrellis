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

import java.nio.ByteBuffer

import monocle.syntax._

import geotrellis.raster.io.geotiff.reader.utils.ByteBufferUtils._

import geotrellis.raster.io.geotiff.reader.GeoKeyDirectoryLenses._

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
      case GTModelTypeGeoKey => geoKeyDirectory |-> gtModelTypeLens set(short)
      case GTRasterTypeGeoKey =>
        geoKeyDirectory |-> gtRasterTypeLens set(Some(short))
      case GeogTypeGeoKey => geoKeyDirectory |-> geogTypeLens set(Some(short))
      case GeogGeodeticDatumGeoKey =>
        geoKeyDirectory |-> geogGeodeticDatumLens set(Some(short))
      case GeogPrimeMeridianGeoKey =>
        geoKeyDirectory |-> geogPrimeMeridianLens set(Some(short))
      case GeogLinearUnitsGeoKey =>
        geoKeyDirectory |-> geogLinearUnitsLens set(Some(short))
      case GeogAngularUnitsGeoKey =>
        geoKeyDirectory |-> geogAngularUnitsLens set(Some(short))
      case GeogEllipsoidGeoKey =>
        geoKeyDirectory |-> geogEllipsoidLens set(Some(short))
      case GeogAzimuthUnitsGeoKey =>
        geoKeyDirectory |-> geogAzimuthUnitsLens set(Some(short))
      case ProjectedCSTypeGeoKey =>
        geoKeyDirectory |-> projectedCSTypeLens set(short)
      case ProjectionGeoKey =>
        geoKeyDirectory |-> projectionLens set(Some(short))
      case ProjCoordTransGeoKey =>
        geoKeyDirectory |-> projCoordTransLens set(Some(short))
      case ProjLinearUnitsGeoKey =>
        geoKeyDirectory |-> projLinearUnitsLens set(Some(short))
      case VerticalCSTypeGeoKey =>
        geoKeyDirectory |-> verticalCSTypeLens set(Some(short))
      case VerticalDatumGeoKey =>
        geoKeyDirectory |-> verticalDatumLens set(Some(short))
      case VerticalUnitsGeoKey =>
        geoKeyDirectory |-> verticalUnitsLens set(Some(short))
      case tag => geoKeyDirectory |-> geoKeyShortMapLens modify (_ + (tag
          -> short))
    }
  }

  private def readDoubles(keyMetadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val doubles = directory.geoTiffTags.doubles.get.drop(
      keyMetadata.valueOffset).take(keyMetadata.count)

    keyMetadata.keyID match {
      case GeogLinearUnitSizeGeoKey =>
        geoKeyDirectory |-> geogLinearUnitSizeLens set(Some(doubles(0)))
      case GeogAngularUnitSizeGeoKey =>
        geoKeyDirectory |-> geogAngularUnitSizeLens set(Some(doubles(0)))
      case GeogSemiMajorAxisGeoKey =>
        geoKeyDirectory |-> geogSemiMajorAxisLens set(Some(doubles(0)))
      case GeogSemiMinorAxisGeoKey =>
        geoKeyDirectory |-> geogSemiMinorAxisLens set(Some(doubles(0)))
      case GeogInvFlatteningGeoKey =>
        geoKeyDirectory |-> geogInvFlatteningLens set(Some(doubles(0)))
      case GeogPrimeMeridianLongGeoKey =>
        geoKeyDirectory |-> geogPrimeMeridianLongLens set(Some(doubles(0)))
      case ProjLinearUnitSizeGeoKey =>
        geoKeyDirectory |-> projLinearUnitSizeLens set(Some(doubles(0)))
      case ProjStdParallel1GeoKey =>
        geoKeyDirectory |-> projStdParallel1Lens set(Some(doubles(0)))
      case ProjStdParallel2GeoKey =>
        geoKeyDirectory |-> projStdParallel2Lens set(Some(doubles(0)))
      case ProjNatOriginLongGeoKey =>
        geoKeyDirectory |-> projNatOriginLongLens set(Some(doubles(0)))
      case ProjNatOriginLatGeoKey =>
        geoKeyDirectory |-> projNatOriginLatLens set(Some(doubles(0)))
      case ProjFalseEastingGeoKey =>
        geoKeyDirectory |-> projFalseEastingLens set(Some(doubles(0)))
      case ProjFalseNorthingGeoKey =>
        geoKeyDirectory |-> projFalseNorthingLens set(Some(doubles(0)))
      case ProjFalseOriginLongGeoKey =>
        geoKeyDirectory |-> projFalseOriginLongLens set(Some(doubles(0)))
      case ProjFalseOriginLatGeoKey =>
        geoKeyDirectory |-> projFalseOriginLatLens set(Some(doubles(0)))
      case ProjFalseOriginEastingGeoKey =>
        geoKeyDirectory |-> projFalseOriginEastingLens set(Some(doubles(0)))
      case ProjFalseOriginNorthingGeoKey =>
        geoKeyDirectory |-> projFalseOriginNorthingLens set(Some(doubles(0)))
      case ProjCenterLongGeoKey =>
        geoKeyDirectory |-> projCenterLongLens set(Some(doubles(0)))
      case ProjCenterLatGeoKey =>
        geoKeyDirectory |-> projCenterLatLens set(Some(doubles(0)))
      case ProjCenterEastingGeoKey =>
        geoKeyDirectory |-> projCenterEastingLens set(Some(doubles(0)))
      case ProjCenterNorthingGeoKey =>
        geoKeyDirectory |-> projCenterNorthingLens set(Some(doubles(0)))
      case ProjScaleAtNatOriginGeoKey =>
        geoKeyDirectory |-> projScaleAtNatOriginLens set(Some(doubles(0)))
      case ProjScaleAtCenterGeoKey =>
        geoKeyDirectory |-> projScaleAtCenterLens set(Some(doubles(0)))
      case ProjAzimuthAngleGeoKey =>
        geoKeyDirectory |-> projAzimuthAngleLens set(Some(doubles(0)))
      case ProjStraightVertPoleLongGeoKey =>
        geoKeyDirectory |-> projStraightVertPoleLongLens set(Some(doubles(0)))
      case ProjRectifiedGridAngleGeoKey =>
        geoKeyDirectory |-> projRectifiedGridAngleLens set(Some(doubles(0)))
      case tag => geoKeyDirectory |-> geoKeyDoublesMapLens modify (_ + (tag
          -> doubles))
    }
  }

  private def readAsciis(metadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val strings = directory.geoTiffTags.asciis.get.substring(
      metadata.valueOffset,
      metadata.count + metadata.valueOffset
    ).split("\\|").toArray

    metadata.keyID match {
      case GTCitationGeoKey =>
        geoKeyDirectory |-> gtCitationLens set(Some(strings))
      case GeogCitationGeoKey =>
        geoKeyDirectory |-> geogCitationLens set(Some(strings))
      case PCSCitationGeoKey =>
        geoKeyDirectory |-> pcsCitationLens set(Some(strings))
      case VerticalCitationGeoKey =>
        geoKeyDirectory |-> verticalCitationLens set(Some(strings))
      case tag => geoKeyDirectory |-> geoKeyAsciisMapLens modify (_ + (tag
          -> strings))
    }
  }

}
