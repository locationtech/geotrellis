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

package geotrellis.io.geotiffreader

case class GTKeyDirectoryReader(asciiParams: GTFieldGeoAsciiParams,
  doubleParams: GTFieldGeoDoubleParams) {

  def read(streamArray: Array[Char], metadata: GTKDMetadata, offset: Int) = {
    val keyEntries = Array.ofDim[GTKeyEntry](metadata.numberOfKeys)

    val getShort = GTReaderUtils.getShort(streamArray)(_)

    for (i <- 0 until metadata.numberOfKeys) {
      val current = offset + i * 8
      val keyID = getShort(current)
      val tiffTagLocation = getShort(current + 2)
      val count = getShort(current + 4)
      val valueOffset = getShort(current + 6)

      println("keyID: " + keyID)
      println("tiffTagLocation: " + tiffTagLocation)
      println("count: " + count)
      println("valueOffset: " + valueOffset)

      val keyEntryMetadata = GTKEMetadata(keyID, tiffTagLocation, count,
        valueOffset)

      val keyEntryData = readKeyEntry(streamArray, keyEntryMetadata,
        current + 8)
      keyEntries(i) = GTKeyEntry(keyEntryMetadata, keyEntryData)
    }

    GTFieldGeoKeyDirectory(metadata, keyEntries)
  }

  private def readKeyEntry(streamArray: Array[Char], metadata: GTKEMetadata,
    offset: Int) = metadata.keyID match {
    case 1024 => parseGTKEModelType(streamArray, metadata, offset)
    case 1025 => parseGTKERasterType(streamArray, metadata, offset)
    case 1026 => parseGTKECitation(streamArray, metadata, offset)
    case 2048 => parseGTKEGeographicType(streamArray, metadata, offset)
    case 2052 => parseGTKEGeogLinearUnits(streamArray, metadata, offset)
    case 2054 => parseGTKEGeogAngularUnits(streamArray, metadata, offset)
    case 3072 => parseGTKEProjectedCSType(streamArray, metadata, offset)
    case 3074 => parseGTKEProjection(streamArray, metadata, offset)
    case 3075 => parseGTKEProjCoordTrans(streamArray, metadata, offset)
    case 3076 => parseGTKEProjLinearUnits(streamArray, metadata, offset)
    case 3078 => parseGTKEProjStdParallel1(streamArray, metadata, offset)
    case 3079 => parseGTKEProjStdParallel2(streamArray, metadata, offset)
    case 3081 => parseGTKEProjNatOriginLat(streamArray, metadata, offset)
    case 3082 => parseGTKEProjFalseEasting(streamArray, metadata, offset)
    case 3083 => parseGTKEProjFalseNorthing(streamArray, metadata, offset)
    case 3088 => parseGTKEProjCenterLong(streamArray, metadata, offset)
  }

  private def parseGTKEModelType(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEModelType(metadata.valueOffset)
  }

  private def parseGTKERasterType(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKERasterType(metadata.valueOffset)
  }

  private def parseGTKECitation(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34737 => GTKECitation(GTReaderUtils.getPartialString(asciiParams.value,
      metadata.valueOffset, metadata.count))
  }

  private def parseGTKEGeographicType(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEGeographicType(metadata.valueOffset)
  }

  private def parseGTKEGeogLinearUnits(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEGeogLinearUnits(metadata.valueOffset)
  }

  private def parseGTKEGeogAngularUnits(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEGeogAngularUnits(metadata.valueOffset)
  }

  private def parseGTKEProjectedCSType(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEProjectedCSType(metadata.valueOffset)
  }

  private def parseGTKEProjection(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEProjection(metadata.valueOffset)
  }

  private def parseGTKEProjCoordTrans(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEProjCoordTrans(metadata.valueOffset)
  }

  private def parseGTKEProjLinearUnits(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 0 => GTKEProjLinearUnits(metadata.valueOffset)
  }

  private def parseGTKEProjStdParallel1(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34736 => GTKEProjStdParallel1(
      doubleParams.values(metadata.valueOffset))
  }

  private def parseGTKEProjStdParallel2(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34736 => GTKEProjStdParallel2(
      doubleParams.values(metadata.valueOffset))
  }

  private def parseGTKEProjNatOriginLat(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34736 => GTKEProjNatOriginLat(
      doubleParams.values(metadata.valueOffset))
  }

  private def parseGTKEProjFalseEasting(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34736 => GTKEProjFalseEasting(
      doubleParams.values(metadata.valueOffset))
  }

  private def parseGTKEProjFalseNorthing(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34736 => GTKEProjFalseNorthing(
      doubleParams.values(metadata.valueOffset))
  }

  private def parseGTKEProjCenterLong(streamArray: Array[Char],
    metadata: GTKEMetadata, offset: Int) = metadata.tiffTagLocation match {
    case 34736 => GTKEProjCenterLong(
      doubleParams.values(metadata.valueOffset))
  }
}
