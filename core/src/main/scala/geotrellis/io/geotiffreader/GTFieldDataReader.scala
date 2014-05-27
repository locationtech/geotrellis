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

object GTFieldDataReader {

  val emptyGTDD = GTDDEmpty()

  def read(streamArray: Array[Char], metadata: GTFieldMetadata,
    gtDeps: GTDDependencies = emptyGTDD) = metadata.tag match {
      case 256 => parseImageWidth(streamArray, metadata)
      case 257 => parseImageHeight(streamArray, metadata)
      case 258 => parseBitsPerSample(streamArray, metadata)
      case 259 => parseCompression(streamArray, metadata)
      case 262 => parsePhotometricInterpretation(streamArray, metadata)
      case 273 => parseStripOffsets(streamArray, metadata)
      case 277 => parseSamplesPerPixel(streamArray, metadata)
      case 278 => parseRowsPerStrip(streamArray, metadata)
      case 279 => parseStripByteCounts(streamArray, metadata)
      case 284 => parsePlanarConfiguration(streamArray, metadata)
      case 33550 => parseModelPixelScale(streamArray, metadata)
      case 33922 => parseModelTiePoints(streamArray, metadata)
      case 34735 => parseGeoKeyDirectory(streamArray, gtDeps)
      case 34736 => parseGeoDoubleParams(streamArray, metadata)
      case 34737 => parseGeoAsciiParams(streamArray, metadata)
    }

  private def parseImageWidth(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldImageWidth(streamArray(metadata.offset))
  }

  private def parseImageHeight(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldImageHeight(streamArray(metadata.offset))
  }

  private def parseBitsPerSample(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldBitsPerSample(streamArray(metadata.offset))
  }

  private def parseCompression(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldCompression(streamArray(metadata.offset))
  }

  private def parsePhotometricInterpretation(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldPhotometricInterpretation(streamArray(metadata.offset))
  }

  private def parseStripOffsets(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val stripOffsets =
      GTReaderUtils.getIntFieldDataArray(streamArray, metadata)

    GTFieldStripOffsets(stripOffsets)
  }

  private def parseSamplesPerPixel(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldSamplesPerPixel(streamArray(metadata.offset))
  }

  private def parseRowsPerStrip(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldRowsPerStrip(streamArray(metadata.offset))
  }

  private def parseStripByteCounts(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val stripByteCounts =
      GTReaderUtils.getIntFieldDataArray(streamArray, metadata)

    GTFieldStripByteCounts(stripByteCounts)
  }

  private def parsePlanarConfiguration(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldPlanarConfiguration(streamArray(metadata.offset))
  }

  private def parseModelPixelScale(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val getDouble = GTReaderUtils.getDouble(streamArray)(_)

    val scaleX = getDouble(metadata.offset)
    val scaleY = getDouble(metadata.offset + 8)
    val scaleZ = getDouble(metadata.offset + 16)

    GTFieldModelPixelScale(scaleX, scaleY, scaleZ)
  }

  private def parseModelTiePoints(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val numberOfPoints = metadata.length / 6
    val points = Array.ofDim[GTModelTiePoint](numberOfPoints)

    val getDouble = GTReaderUtils.getDouble(streamArray)(_)

    for (index <- 0 until numberOfPoints) {
      val current = metadata.offset + index * 6 * 8

      val i = getDouble(current)
      val j = getDouble(current + 8)
      val k = getDouble(current + 16)
      val x = getDouble(current + 24)
      val y = getDouble(current + 32)
      val z = getDouble(current + 40)

      points(index) = GTModelTiePoint(i, j, k, x, y, z)
    }

    GTFieldModelTiePoints(points)
  }

  private def parseGeoKeyDirectory(streamArray: Array[Char],
    gtDeps: GTDDependencies) = gtDeps match {
    case GTDDComplete(asciiParams, doubleParams, metadata) => {
      val getShort = GTReaderUtils.getShort(streamArray)(_)

      val version = getShort(metadata.offset)
      val keyRevision = getShort(metadata.offset + 2)
      val minorRevision = getShort(metadata.offset + 4)
      val numberOfKeys = getShort(metadata.offset + 6)

      val directoryMetadata = GTKDMetadata(version, keyRevision, minorRevision,
        numberOfKeys)

      val gtKeyDirectoryReader = GTKeyDirectoryReader(asciiParams, doubleParams)

      gtKeyDirectoryReader.read(streamArray, directoryMetadata,
        metadata.offset + 8)
    }
  }

  private def parseGeoDoubleParams(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val array = GTReaderUtils.getDoubleFieldDataArray(streamArray, metadata)
    GTFieldGeoDoubleParams(array)
  }

  private def parseGeoAsciiParams(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    val array = GTReaderUtils.getString(streamArray)(metadata.offset,
      metadata.length)
    GTFieldGeoAsciiParams(array)
  }

}
