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

object GeoTiffFieldDataReader {

  def read(streamArray: Array[Char], metadata: GTFieldMetadata) =
    metadata.tag match {
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
      GeoTiffReaderUtils.getFieldDataArray(streamArray, metadata)

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
      GeoTiffReaderUtils.getFieldDataArray(streamArray, metadata)

    GTFieldStripByteCounts(stripByteCounts)
  }

  private def parsePlanarConfiguration(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {
    GTFieldPlanarConfiguration(streamArray(metadata.offset))
  }

  private def parseModelPixelScale(streamArray: Array[Char],
    metadata: GTFieldMetadata) = {

  }
}
