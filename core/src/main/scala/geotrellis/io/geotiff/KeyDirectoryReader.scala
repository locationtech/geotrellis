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

case class KeyDirectoryReader(byteBuffer: ByteBuffer,
  directory: ImageDirectory) {

  def read(geoKeyDirectory: GeoKeyDirectory, index: Int = 0):
      GeoKeyDirectory = index match {
    case geoKeyDirectory.count => geoKeyDirectory
    case _ => {
      val keyEntryMetadata = KeyMetadata(
        byteBuffer.getShort,
        byteBuffer.getShort,
        byteBuffer.getShort,
        byteBuffer.getShort
      )

      val updatedDirectory = readKeyEntry(keyEntryMetadata, geoKeyDirectory)

      read(updatedDirectory, index + 1)
    }
  }

  private def readKeyEntry(keyMetadata: KeyMetadata,
    geoKeyDirectory: GeoKeyDirectory)  =
    (keyMetadata.tiffTagLocation,  keyMetadata.count) match {
      case (0, _) => readShort(keyMetadata, geoKeyDirectory)
      case (34735, _) => readShorts(keyMetadata, geoKeyDirectory)
      case (34736, _) => readDoubles(keyMetadata, geoKeyDirectory)
      case (34737, _) => readAsciis(keyMetadata, geoKeyDirectory)
    }

  private def readShort(keyMetadata: KeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = keyMetadata.keyID match {
    case 1024 => geoKeyDirectory.copy(configKeys =
      geoKeyDirectory.configKeys.copy(gtModelType =
        Some(keyMetadata.valueOffset)))
    case 1025 => geoKeyDirectory.copy(configKeys =
      geoKeyDirectory.configKeys.copy(gtRasterType =
        Some(keyMetadata.valueOffset)))
    case 2048 => geoKeyDirectory.copy(geogCSParameterKeys =
      geoKeyDirectory.geogCSParameterKeys.copy(geogType =
        Some(keyMetadata.valueOffset)))
    case 2052 => geoKeyDirectory.copy(geogCSParameterKeys =
      geoKeyDirectory.geogCSParameterKeys.copy(geogLinearUnits =
        Some(keyMetadata.valueOffset)))
    case 2054 => geoKeyDirectory.copy(geogCSParameterKeys =
      geoKeyDirectory.geogCSParameterKeys.copy(geogAngularUnits =
        Some(keyMetadata.valueOffset)))
    case 3072 => geoKeyDirectory.copy(projectedCSParameterKeys = geoKeyDirectory.
        projectedCSParameterKeys.copy(projectedCSType =
          Some(keyMetadata.valueOffset)))
    case 3074 => geoKeyDirectory.copy(projectedCSParameterKeys = geoKeyDirectory.
        projectedCSParameterKeys.copy(projection =
          Some(keyMetadata.valueOffset)))
    case 3075 => geoKeyDirectory.copy(projectedCSParameterKeys = geoKeyDirectory.
        projectedCSParameterKeys.copy(projCoordTrans =
          Some(keyMetadata.valueOffset)))
    case 3076 => geoKeyDirectory.copy(projectedCSParameterKeys = geoKeyDirectory.
        projectedCSParameterKeys.copy(projLinearUnits =
          Some(keyMetadata.valueOffset)))
  }

  private def readShorts(keyMetadata: KeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = geoKeyDirectory

  private def readDoubles(keyMetadata: KeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val doubles = directory.geoTiffTags.doubles.get.drop(
      keyMetadata.valueOffset).take(keyMetadata.count)

    keyMetadata.keyID match {
      case 2059 => geoKeyDirectory.copy(geogCSParameterKeys = geoKeyDirectory.
          geogCSParameterKeys.copy(geogInvFlattening = Some(doubles)))
      case 3078 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projStdparallel1 =
          Some(doubles)))
      case 3079 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projStdparallel2 =
          Some(doubles)))
      case 3080 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projNatOriginLong =
          Some(doubles)))
      case 3081 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projNatOriginLat =
          Some(doubles)))
      case 3082 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projectedFalsings =
          geoKeyDirectory.projectedCSParameterKeys.projectedFalsings.copy(
            projFalseEasting = Some(doubles))))
      case 3083 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projectedFalsings =
          geoKeyDirectory.projectedCSParameterKeys.projectedFalsings.copy(
            projFalseNorthing = Some(doubles))))
      case 3084 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projectedFalsings =
          geoKeyDirectory.projectedCSParameterKeys.projectedFalsings.copy(
            projFalseOriginLong = Some(doubles))))
      case 3085 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projectedFalsings =
          geoKeyDirectory.projectedCSParameterKeys.projectedFalsings.copy(
            projFalseOriginLat = Some(doubles))))
      case 3086 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projectedFalsings =
          geoKeyDirectory.projectedCSParameterKeys.projectedFalsings.copy(
            projFalseOriginEasting = Some(doubles))))
      case 3087 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projectedFalsings =
          geoKeyDirectory.projectedCSParameterKeys.projectedFalsings.copy(
            projFalseOriginNorthing = Some(doubles))))
      case 3088 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projCenterLong =
          Some(doubles)))
      case 3089 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projCenterLat =
          Some(doubles)))
      case 3090 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projCenterEasting =
          Some(doubles)))
      case 3091 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projCenterNorthing =
          Some(doubles)))
      case 3092 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projScaleAtNatOrigin =
          Some(doubles)))
      case 3093 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projScaleAtCenter =
          Some(doubles)))
      case 3094 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projAzimuthAngle =
          Some(doubles)))
      case 3095 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(projStraightVertpoleLong
          = Some(doubles)))
    }
  }

  private def readAsciis(metadata: KeyMetadata,
    geoKeyDirectory: GeoKeyDirectory) = {
    val strings = directory.geoTiffTags.asciis.get.substring(
      metadata.valueOffset,
      metadata.count
    ).split("\\|").toVector

    metadata.keyID match {
      case 1026 => geoKeyDirectory.copy(configKeys = geoKeyDirectory.configKeys.
          copy(gtCitation = Some(strings)))
      case 2049 => geoKeyDirectory.copy(geogCSParameterKeys = geoKeyDirectory.
          geogCSParameterKeys.copy(geogCitation = Some(strings)))
      case 3073 => geoKeyDirectory.copy(projectedCSParameterKeys =
        geoKeyDirectory.projectedCSParameterKeys.copy(pcsCitation =
          Some(strings)))
    }
  }

}
