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

import ReaderUtils._

case class KeyDirectoryReader(asciis: String, doubles: Array[Double]) {

  def read(streamArray: Array[Char], offset: Int, directory:
      GeoKeyDirectory, index: Int): GeoKeyDirectory = index match {
    case directory.count => directory
    case _ => {
      val getShortValue = getShort(streamArray)(_)

      val current = offset + index * 8
      val keyID = getShortValue(current)
      val tiffTagLocation = getShortValue(current + 2)
      val count = getShortValue(current + 4)
      val valueOffset = getShortValue(current + 6)

      val keyEntryMetadata = KeyMetadata(keyID, tiffTagLocation, count,
        valueOffset)

      val newDirectory = readKeyEntry(streamArray, keyEntryMetadata,
        current + 8, directory)

      read(streamArray, offset, newDirectory, index + 1)
    }
  }

  private def readKeyEntry(streamArray: Array[Char], metadata: KeyMetadata,
    offset: Int, directory: GeoKeyDirectory) = (metadata.tiffTagLocation,
      metadata.count) match {
    case (0, _) => readShort(metadata.keyID, metadata.valueOffset, directory)
    case (34735, _) => readShorts(metadata, offset, directory)
    case (34736, _) => readDoubles(metadata, directory)
    case (34737, _) => readAsciis(metadata, directory)
  }

  private def readShort(keyID: Int, value: Int, directory:
      GeoKeyDirectory) = keyID match {
    case 1024 => directory.copy(configKeys = directory.configKeys.copy(
      gtModelType = Some(value)))
    case 1025 => directory.copy(configKeys = directory.configKeys.copy(
      gtRasterType = Some(value)))
    case 2048 => directory.copy(geogCSParameterKeys =
      directory.geogCSParameterKeys.copy(geogType = Some(value)))
    case 2052 => directory.copy(geogCSParameterKeys =
      directory.geogCSParameterKeys.copy(geogLinearUnits = Some(value)))
    case 2054 => directory.copy(geogCSParameterKeys =
      directory.geogCSParameterKeys.copy(geogAngularUnits = Some(value)))
    case 3072 => directory.copy(projectedCSParameterKeys = directory.
        projectedCSParameterKeys.copy(projectedCSType = Some(value)))
    case 3074 => directory.copy(projectedCSParameterKeys = directory.
        projectedCSParameterKeys.copy(projection = Some(value)))
    case 3075 => directory.copy(projectedCSParameterKeys = directory.
        projectedCSParameterKeys.copy(projCoordTrans = Some(value)))
    case 3076 => directory.copy(projectedCSParameterKeys = directory.
        projectedCSParameterKeys.copy(projLinearUnits = Some(value)))

  }

  private def readShorts(metadata: KeyMetadata, offset: Int, directory:
      GeoKeyDirectory) = {
    directory
  }

  private def readDoubles(metadata: KeyMetadata, directory: GeoKeyDirectory) = {
    val doubleArray = doubles.drop(metadata.valueOffset).take(metadata.count)

    metadata.keyID match {
      case 2059 => directory.copy(geogCSParameterKeys = directory.
          geogCSParameterKeys.copy(geogInvFlattening = Some(doubleArray)))
      case 3078 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projStdparallel1 = Some(doubleArray)))
      case 3079 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projStdparallel2 = Some(doubleArray)))
      case 3080 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projNatOriginLong = Some(doubleArray)))
      case 3081 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projNatOriginLat = Some(doubleArray)))
      case 3082 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projectedFalsings = directory.
            projectedCSParameterKeys.projectedFalsings.copy(projFalseEasting
              = Some(doubleArray))))
      case 3083 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projectedFalsings = directory.
            projectedCSParameterKeys.projectedFalsings.copy(projFalseNorthing
              = Some(doubleArray))))
      case 3084 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projectedFalsings = directory.
            projectedCSParameterKeys.projectedFalsings.copy(projFalseOriginLong
              = Some(doubleArray))))
      case 3085 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projectedFalsings = directory.
            projectedCSParameterKeys.projectedFalsings.copy(projFalseOriginLat
              = Some(doubleArray))))
      case 3086 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projectedFalsings = directory.
            projectedCSParameterKeys.projectedFalsings.copy(
              projFalseOriginEasting = Some(doubleArray))))
      case 3087 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projectedFalsings = directory.
            projectedCSParameterKeys.projectedFalsings.copy(
              projFalseOriginNorthing = Some(doubleArray))))
      case 3088 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projCenterLong = Some(doubleArray)))
      case 3089 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projCenterLat = Some(doubleArray)))
      case 3090 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projCenterEasting = Some(doubleArray)))
      case 3091 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projCenterNorthing = Some(doubleArray)))
      case 3092 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projScaleAtNatOrigin =
            Some(doubleArray)))
      case 3093 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projScaleAtCenter = Some(doubleArray)))
      case 3094 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projAzimuthAngle = Some(doubleArray)))
      case 3095 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(projStraightVertpoleLong
            = Some(doubleArray)))
    }
  }

  private def readAsciis(metadata: KeyMetadata, directory: GeoKeyDirectory) = {
    val stringArray = getPartialString(asciis, metadata.valueOffset,
      metadata.count)

    metadata.keyID match {
      case 1026 => directory.copy(configKeys = directory.configKeys.
          copy(gtCitation = Some(stringArray)))
      case 2049 => directory.copy(geogCSParameterKeys = directory.
          geogCSParameterKeys.copy(geogCitation = Some(stringArray)))
      case 3073 => directory.copy(projectedCSParameterKeys = directory.
          projectedCSParameterKeys.copy(pcsCitation = Some(stringArray)))
    }
  }

}
