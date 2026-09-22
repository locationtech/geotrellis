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

package geotrellis.raster.io.geotiff.tags

import geotrellis.raster.io.geotiff.util.*
import codes.TagCodes.*

import GeoKeys.*

import geotrellis.util.ByteReader

import monocle.syntax.all.*

object GeoKeyReader {

  def read(byteReader: ByteReader, imageDirectory: TiffTags,
    geoKeyDirectory: GeoKeyDirectory, index: Int = 0
  ): GeoKeyDirectory = {

    /**
      * Attempt to read GeoKey Entry; if tag does not match then assume there are no
      * more valid geokeys and the header was written incorrectly so return `None`
      */
    def readGeoKeyEntry(keyMetadata: GeoKeyMetadata,
      geoKeyDirectory: GeoKeyDirectory): Option[GeoKeyDirectory] = keyMetadata.tiffTagLocation match {
      case 0 => Some(readShort(keyMetadata, geoKeyDirectory))
      case DoublesTag => Some(readDoubles(keyMetadata, geoKeyDirectory))
      case AsciisTag => Some(readAsciis(keyMetadata, geoKeyDirectory))
      case _ => None
    }

    def readShort(keyMetadata: GeoKeyMetadata,
      geoKeyDirectory: GeoKeyDirectory) = {
      val short = keyMetadata.valueOffset

      keyMetadata.keyID match {
        case GTModelTypeGeoKey => geoKeyDirectory.focus(_.configKeys.gtModelType).replace(short)
        case GTRasterTypeGeoKey => geoKeyDirectory.focus(_.configKeys.gtRasterType).replace(Some(short))
        case GeogTypeGeoKey => geoKeyDirectory.focus(_.geogCSParameterKeys.geogType).replace(Some(short))
        case GeogGeodeticDatumGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogGeodeticDatum).replace(Some(short))
        case GeogPrimeMeridianGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogPrimeMeridian).replace(Some(short))
        case GeogLinearUnitsGeoKey => geoKeyDirectory.focus(_.geogCSParameterKeys.geogLinearUnits).replace(Some(short))
        case GeogAngularUnitsGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogAngularUnits).replace(Some(short))
        case GeogEllipsoidGeoKey => geoKeyDirectory.focus(_.geogCSParameterKeys.geogEllipsoid).replace(Some(short))
        case GeogAzimuthUnitsGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogAzimuthUnits).replace(Some(short))
        case ProjectedCSTypeGeoKey => geoKeyDirectory.focus(_.projectedCSParameterKeys.projectedCSType).replace(short)
        case ProjectionGeoKey => geoKeyDirectory.focus(_.projectedCSParameterKeys.projection).replace(Some(short))
        case ProjCoordTransGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projCoordTrans).replace(Some(short))
        case ProjLinearUnitsGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projLinearUnits).replace(Some(short))
        case VerticalCSTypeGeoKey => geoKeyDirectory.focus(_.verticalCSKeys.verticalCSType).replace(Some(short))
        case VerticalDatumGeoKey => geoKeyDirectory.focus(_.verticalCSKeys.verticalDatum).replace(Some(short))
        case VerticalUnitsGeoKey => geoKeyDirectory.focus(_.verticalCSKeys.verticalUnits).replace(Some(short))
        case tag => geoKeyDirectory.focus(_.nonStandardizedKeys.shortMap).modify(
            _ + (tag -> short)
          )
      }
    }

    def readDoubles(keyMetadata: GeoKeyMetadata,
      geoKeyDirectory: GeoKeyDirectory) = {
      val doubles = imageDirectory
        .geoTiffTags
        .doubles
        .get
        .drop(keyMetadata.valueOffset)
        .take(keyMetadata.count)

      keyMetadata.keyID match {
        case GeogLinearUnitSizeGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogLinearUnitSize).replace(Some(doubles(0)))
        case GeogAngularUnitSizeGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogAngularUnitSize).replace(Some(doubles(0)))
        case GeogSemiMajorAxisGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogSemiMajorAxis).replace(Some(doubles(0)))
        case GeogSemiMinorAxisGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogSemiMinorAxis).replace(Some(doubles(0)))
        case GeogInvFlatteningGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogInvFlattening).replace(Some(doubles(0)))
        case GeogPrimeMeridianLongGeoKey =>
          geoKeyDirectory.focus(_.geogCSParameterKeys.geogPrimeMeridianLong).replace(Some(doubles(0)))
        case ProjLinearUnitSizeGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projLinearUnitSize).replace(Some(doubles(0)))
        case ProjStdParallel1GeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projStdParallel1).replace(Some(doubles(0)))
        case ProjStdParallel2GeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projStdParallel2).replace(Some(doubles(0)))
        case ProjNatOriginLongGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projNatOriginLong).replace(Some(doubles(0)))
        case ProjNatOriginLatGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projNatOriginLat).replace(Some(doubles(0)))
        case ProjFalseEastingGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projectedFalsings.projFalseEasting).replace(Some(doubles(0)))
        case ProjFalseNorthingGeoKey =>
          geoKeyDirectory
            .focus(_.projectedCSParameterKeys.projectedFalsings.projFalseNorthing)
            .replace(Some(doubles(0)))
        case ProjFalseOriginLongGeoKey =>
          geoKeyDirectory
            .focus(_.projectedCSParameterKeys.projectedFalsings.projFalseOriginLong)
            .replace(Some(doubles(0)))
        case ProjFalseOriginLatGeoKey =>
          geoKeyDirectory
            .focus(_.projectedCSParameterKeys.projectedFalsings.projFalseOriginLat)
            .replace(Some(doubles(0)))
        case ProjFalseOriginEastingGeoKey =>
          geoKeyDirectory
            .focus(_.projectedCSParameterKeys.projectedFalsings.projFalseOriginEasting)
            .replace(Some(doubles(0)))
        case ProjFalseOriginNorthingGeoKey =>
          geoKeyDirectory
            .focus(_.projectedCSParameterKeys.projectedFalsings.projFalseOriginNorthing)
            .replace(Some(doubles(0)))
        case ProjCenterLongGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projCenterLong).replace(Some(doubles(0)))
        case ProjCenterLatGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projCenterLat).replace(Some(doubles(0)))
        case ProjCenterEastingGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projCenterEasting).replace(Some(doubles(0)))
        case ProjCenterNorthingGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projCenterNorthing).replace(Some(doubles(0)))
        case ProjScaleAtNatOriginGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projScaleAtNatOrigin).replace(Some(doubles(0)))
        case ProjScaleAtCenterGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projScaleAtCenter).replace(Some(doubles(0)))
        case ProjAzimuthAngleGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projAzimuthAngle).replace(Some(doubles(0)))
        case ProjStraightVertPoleLongGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projStraightVertPoleLong).replace(Some(doubles(0)))
        case ProjRectifiedGridAngleGeoKey =>
          geoKeyDirectory.focus(_.projectedCSParameterKeys.projRectifiedGridAngle).replace(Some(doubles(0)))
        case tag => geoKeyDirectory.focus(_.nonStandardizedKeys.doublesMap).modify(
            _ + (tag -> doubles)
          )
      }
    }

    def readAsciis(metadata: GeoKeyMetadata,
      geoKeyDirectory: GeoKeyDirectory) = {

      val strings = imageDirectory
        .geoTiffTags
        .asciis
        .get
        .substring(metadata.valueOffset, metadata.count + metadata.valueOffset)
        .split("\\|")
        .toArray

      metadata.keyID match {
        case GTCitationGeoKey => geoKeyDirectory.focus(_.configKeys.gtCitation).replace(Some(strings))
        case GeogCitationGeoKey => geoKeyDirectory.focus(_.geogCSParameterKeys.geogCitation).replace(Some(strings))
        case PCSCitationGeoKey => geoKeyDirectory.focus(_.projectedCSParameterKeys.pcsCitation).replace(Some(strings))
        case VerticalCitationGeoKey => geoKeyDirectory.focus(_.verticalCSKeys.verticalCitation).replace(Some(strings))
        case tag => geoKeyDirectory.focus(_.nonStandardizedKeys.asciisMap).modify(
            _ + (tag -> strings)
          )
      }
    }


    index match {
      case geoKeyDirectory.count => geoKeyDirectory
      case _ => {
        val keyEntryMetadata = GeoKeyMetadata(
          byteReader.getUnsignedShort,
          byteReader.getUnsignedShort,
          byteReader.getUnsignedShort,
          byteReader.getUnsignedShort
        )
        readGeoKeyEntry(keyEntryMetadata, geoKeyDirectory) match {
          case Some(updatedDirectory) => read(byteReader, imageDirectory, updatedDirectory, index + 1)
          case None => geoKeyDirectory
        }
      }
    }
  }

}
