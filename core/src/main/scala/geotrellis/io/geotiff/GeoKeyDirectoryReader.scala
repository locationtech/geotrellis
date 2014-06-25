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

package geotrellis.io.geotiff

import java.nio.ByteBuffer

import monocle.syntax._

import geotrellis.io.geotiff.utils.ByteBufferUtils._

import geotrellis.io.geotiff.GeoKeyDirectoryLenses._

case class GeoKeyDirectoryReader(byteBuffer: ByteBuffer,
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
    geoKeyDirectory: GeoKeyDirectory)  =
    (keyMetadata.tiffTagLocation,  keyMetadata.count) match {
      case (0, _) => readShort(keyMetadata, geoKeyDirectory)
      case (34736, _) => readDoubles(keyMetadata, geoKeyDirectory)
      case (34737, _) => readAsciis(keyMetadata, geoKeyDirectory)
    }

  private def readShort(keyMetadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val short = byteBuffer.getUnsignedShort(keyMetadata.valueOffset)

    keyMetadata.keyID match {
      case 1024 => geoKeyDirectory |-> gtModelTypeLens set(Some(short))
      case 1025 => geoKeyDirectory |-> gtRasterTypeLens set(Some(short))
      case 2048 => geoKeyDirectory |-> geogTypeLens set(Some(short))
      case 2050 => geoKeyDirectory |-> geogGeodeticDatumLens set(Some(short))
      case 2051 => geoKeyDirectory |-> geogPrimeMeridianLens set(Some(short))
      case 2052 => geoKeyDirectory |-> geogLinearUnitsLens set(Some(short))
      case 2054 => geoKeyDirectory |-> geogAngularUnitsLens set(Some(short))
      case 2056 => geoKeyDirectory |-> geogEllipsoidLens set(Some(short))
      case 2060 => geoKeyDirectory |-> geogAzimuthUnitsLens set(Some(short))
      case 3072 => geoKeyDirectory |-> projectedCSTypeLens set(Some(short))
      case 3074 => geoKeyDirectory |-> projectionLens set(Some(short))
      case 3075 => geoKeyDirectory |-> projCoordTransLens set(Some(short))
      case 3076 => geoKeyDirectory |-> projLinearUnitsLens set(Some(short))
      case 4096 => geoKeyDirectory |-> verticalCSTypeLens set(Some(short))
      case 4098 => geoKeyDirectory |-> verticalDatumLens set(Some(short))
      case 4099 => geoKeyDirectory |-> verticalUnitsLens set(Some(short))
      case tag => geoKeyDirectory |-> shortMapLens modify (_ + (tag
          -> short))
    }
  }

  private def readDoubles(keyMetadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val doubles = directory.geoTiffTags.doubles.get.drop(
      keyMetadata.valueOffset).take(keyMetadata.count)

    keyMetadata.keyID match {
      case 2053 => geoKeyDirectory |-> geogLinearUnitsSizeLens set(Some(
        doubles))
      case 2055 => geoKeyDirectory |-> geogAngularUnitsSizeLens set(Some(
        doubles))
      case 2057 => geoKeyDirectory |-> geogSemiMajorAxisLens set(Some(doubles))
      case 2058 => geoKeyDirectory |-> geogSemiMinorAxisLens set(Some(doubles))
      case 2059 => geoKeyDirectory |-> geogInvFlatteningLens set(Some(doubles))
      case 2061 => geoKeyDirectory |-> geogPrimeMeridianLongLens set(
        Some(doubles))
      case 3077 => geoKeyDirectory |-> projLinearUnitsSizeLens set(
        Some(doubles))
      case 3078 => geoKeyDirectory |-> projStdparallel1Lens set(Some(doubles))
      case 3079 => geoKeyDirectory |-> projStdparallel2Lens set(Some(doubles))
      case 3080 => geoKeyDirectory |-> projNatOriginLongLens set(Some(doubles))
      case 3081 => geoKeyDirectory |-> projNatOriginLatLens set(Some(doubles))
      case 3082 => geoKeyDirectory |-> projFalseEastingLens set(Some(doubles))
      case 3083 => geoKeyDirectory |-> projFalseNorthingLens set(Some(doubles))
      case 3084 => geoKeyDirectory |-> projFalseOriginLongLens set(
        Some(doubles))
      case 3085 => geoKeyDirectory |-> projFalseOriginLatLens set(Some(doubles))
      case 3086 => geoKeyDirectory |-> projFalseOriginEastingLens set(
        Some(doubles))
      case 3087 => geoKeyDirectory |-> projFalseOriginNorthingLens set(
        Some(doubles))
      case 3088 => geoKeyDirectory |-> projCenterLongLens set(Some(doubles))
      case 3089 => geoKeyDirectory |-> projCenterLatLens set(Some(doubles))
      case 3090 => geoKeyDirectory |-> projCenterEastingLens set(Some(doubles))
      case 3091 => geoKeyDirectory |-> projCenterNorthingLens set(Some(doubles))
      case 3092 => geoKeyDirectory |-> projScaleAtNatOriginLens set(
        Some(doubles))
      case 3093 => geoKeyDirectory |-> projScaleAtCenterLens set(Some(doubles))
      case 3094 => geoKeyDirectory |-> projAzimuthAngleLens set(Some(doubles))
      case 3095 => geoKeyDirectory |-> projStraightVertpoleLongLens set(
        Some(doubles))
      case tag => geoKeyDirectory |-> doublesMapLens modify (_ + (tag
          -> doubles))
    }
  }

  private def readAsciis(metadata: GeoKeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val strings = directory.geoTiffTags.asciis.get.substring(
      metadata.valueOffset,
      metadata.count + metadata.valueOffset
    ).split("\\|").toVector

    metadata.keyID match {
      case 1026 => geoKeyDirectory |-> gtCitationLens set(Some(strings))
      case 2049 => geoKeyDirectory |-> geogCitationLens set(Some(strings))
      case 3073 => geoKeyDirectory |-> pcsCitationLens set(Some(strings))
      case 4097 => geoKeyDirectory |-> verticalCitationLens set(Some(strings))
      case tag => geoKeyDirectory |-> asciisMapLens modify (_ + (tag
          -> strings))
    }
  }

}
