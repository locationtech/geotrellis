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

import GTReaderUtils._

object GTFieldDataReader {

  def read(streamArray: Array[Char], metadata: TagMetadata,
    tags: IFDTags) = (metadata.tag, metadata.fieldType, metadata.length) match {
    case (33550, _, _) => readModelPixelScaleTag(streamArray, metadata, tags)
    case (33922, _, _) => readModelTiePointsTag(streamArray, metadata, tags)
    case (34735, _, _) => readGeoKeyDirectoryTag(streamArray, metadata, tags)
    case (_, 1, _) => readByteArrayTag(streamArray, metadata, tags)
    case (_, 2, _) => readAsciiArrayTag(streamArray, metadata, tags)
    case (_, 3, 1) => readShortTag(streamArray, metadata, tags)
    case (_, 3, _) => readShortArrayTag(streamArray, metadata, tags)
    case (_, 4, 1) => readIntTag(streamArray, metadata, tags)
    case (_, 4, _) => readIntArrayTag(streamArray, metadata, tags)
    case (_, 5, 1) => readFractionalTag(streamArray, metadata, tags)
    case (_, 5, _) => readFractionalArrayTag(streamArray, metadata, tags)
    case (_, 12, _) => readDoubleArrayTag(streamArray, metadata, tags)
  }

  private def readByteArrayTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val bytes = metadata.length match {
      case x if (x <= 4) => getBytes(metadata.offset).take(x)
      case x => getIntDataArray(streamArray, metadata)
    }

    metadata.tag match {
      case 336 => tags.copy(cmykTags = tags.cmykTags.copy(dotRange =
        Some(bytes)))
      case 338 => tags.copy(nonBasics = tags.nonBasics.copy(extraSamples
          = Some(bytes)))
    }
  }

  private def readAsciiArrayTag(streamArray: Array[Char], metadata:
      TagMetadata, tags: IFDTags) = {
    val string = getString(streamArray)(metadata.offset,
      metadata.length)

    metadata.tag match {
      case 270 => tags.copy(metadata =
        tags.metadata.copy(imageDesc = Some(string)))
      case 271 => tags.copy(metadata = tags.metadata.copy(maker = Some(string)))
      case 272 => tags.copy(metadata = tags.metadata.copy(model = Some(string)))
      case 305 => tags.copy(metadata =
        tags.metadata.copy(software = Some(string)))
      case 315 => tags.copy(metadata =
        tags.metadata.copy(artist = Some(string)))
      case 316 => tags.copy(metadata =
        tags.metadata.copy(computer = Some(string)))
      case 33432 => tags.copy(metadata =
        tags.metadata.copy(copyright = Some(string)))
      case 34737 => tags.copy(geoTiffTags =
        tags.geoTiffTags.copy(asciis = Some(string)))
    }
  }

  private def readShortTag(streamArray: Array[Char], metadata: TagMetadata,
    tags: IFDTags) = {
    val short = metadata.offset

    metadata.tag match {
      case 255 => tags.copy(nonBasics =
        tags.nonBasics.copy(subfileType = Some(short)))
      case 256 => tags.copy(basics = tags.basics.copy(imageWidth = Some(short)))
      case 257 => tags.copy(basics = tags.basics.copy(imageLength = Some(short)))
      case 258 => tags.copy(basics = tags.basics.copy(bitsPerSample =
        Array(short)))
      case 259 => tags.copy(basics = tags.basics.copy(compression = short))
      case 262 => tags.copy(basics = tags.basics.copy(photometricInterp
          = Some(short)))
      case 263 => tags.copy(nonBasics = tags.nonBasics.copy(thresholding =
        short))
      case 264 => tags.copy(nonBasics = tags.nonBasics.copy(cellWidth
          = Some(short)))
      case 265 => tags.copy(nonBasics = tags.nonBasics.copy(cellLength
          = Some(short)))
      case 266 => tags.copy(nonBasics = tags.nonBasics.copy(fillOrder
          = Some(short)))
      case 274 => tags.copy(nonBasics = tags.nonBasics.copy(orientation
          = Some(short)))
      case 277 => tags.copy(basics = tags.basics.copy(samplesPerPixel = short))
      case 278 => tags.copy(basics = tags.basics.copy(rowsPerStrip = short))
      case 284 => tags.copy(nonBasics = tags.nonBasics.copy(planarConfiguration
          = Some(short)))
      case 290 => tags.copy(nonBasics = tags.nonBasics.copy(grayResponseUnit
          = Some(short)))
      case 296 => tags.copy(basics = tags.basics.copy(resolutionUnit =
        Some(short)))
      case 317 => tags.copy(nonBasics = tags.nonBasics.copy(predictor
          = Some(short)))
      case 322 => tags.copy(tilesTags = tags.tilesTags.copy(tileWidth
          = Some(short)))
      case 323 => tags.copy(tilesTags = tags.tilesTags.copy(tileLength
          = Some(short)))
      case 332 => tags.copy(cmykTags = tags.cmykTags.copy(inkSet
          = Some(short)))
      case 334 => tags.copy(cmykTags = tags.cmykTags.copy(numberOfInks
          = Some(short)))
      case 512 => tags.copy(jpegTags = tags.jpegTags.copy(jpegProc
          = Some(short)))
      case 513 => tags.copy(jpegTags = tags.jpegTags.copy(
        jpegInterchangeFormat = Some(short)))
      case 514 => tags.copy(jpegTags = tags.jpegTags.copy(
        jpegInterchangeFormatLength = Some(short)))
      case 515 => tags.copy(jpegTags = tags.jpegTags.copy(jpegRestartInterval
          = Some(short)))
      case 531 => tags.copy(yCbCrTags = tags.yCbCrTags.copy(yCbCrPositioning
          = Some(short)))
    }
  }

  private def readShortArrayTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val shorts = metadata.length match {
      case x if (x > 2) => getIntDataArray(streamArray, metadata)
      case x => getShorts(metadata.offset).take(x)
    }

    metadata.tag match {
      case 258 => tags.copy(basics = tags.basics.copy(bitsPerSample = shorts))
      case 273 => tags.copy(basics = tags.basics.copy(stripOffsets
          = Some(shorts)))
      case 279 => tags.copy(basics = tags.basics.copy(stripByteCounts
          = Some(shorts)))
      case 280 => tags.copy(dataSampleFormatTags =
        tags.dataSampleFormatTags.copy(minSampleValues = Some(shorts)))
      case 281 => tags.copy(dataSampleFormatTags =
        tags.dataSampleFormatTags.copy(maxSampleValues = Some(shorts)))
      case 291 => tags.copy(nonBasics = tags.nonBasics.copy(grayResponseCurve
          = Some(shorts)))
      case 297 => tags.copy(documentationTags = tags.documentationTags.copy(
        pageNumbers = Some(shorts)))
      case 301 => tags.copy(colimetryTags = tags.colimetryTags.copy(
        transferFunction = Some(shorts)))
      case 320 => tags.copy(basics = tags.basics.copy(colorMap = Some(shorts)))
      case 321 => tags.copy(nonBasics = tags.nonBasics.copy(halftoneHints
          = Some(shorts)))
      case 325 => tags.copy(tilesTags = tags.tilesTags.copy(tileByteCounts
          = Some(shorts)))
      case 336 => tags.copy(cmykTags = tags.cmykTags.copy(dotRange =
        Some(shorts)))
      case 339 => tags.copy(dataSampleFormatTags =
        tags.dataSampleFormatTags.copy(sampleFormat = Some(shorts)))
      case 342 => tags.copy(colimetryTags = tags.colimetryTags.copy(
        transferRange = Some(shorts)))
      case 517 => tags.copy(jpegTags = tags.jpegTags.copy(
        jpegLosslessPredictors = Some(shorts)))
      case 518 => tags.copy(jpegTags = tags.jpegTags.copy(
        jpegPointTransforms = Some(shorts)))
    }
  }

  private def readIntTag(streamArray: Array[Char], metadata: TagMetadata,
    tags: IFDTags) = {
    val int = metadata.offset

    metadata.tag match {
      case 254 => tags.copy(nonBasics =
        tags.nonBasics.copy(newSubfileType = Some(int)))
      case 256 => tags.copy(basics = tags.basics.copy(imageWidth = Some(int)))
      case 257 => tags.copy(basics = tags.basics.copy(imageLength = Some(int)))
      case 292 => tags.copy(nonBasics =
        tags.nonBasics.copy(t4Options = Some(int)))
      case 293 => tags.copy(nonBasics =
        tags.nonBasics.copy(t6Options = Some(int)))
      case 322 => tags.copy(tilesTags = tags.tilesTags.copy(tileWidth
          = Some(int)))
      case 323 => tags.copy(tilesTags = tags.tilesTags.copy(tileLength
          = Some(int)))
      case 513 => tags.copy(jpegTags = tags.jpegTags.copy(
        jpegInterchangeFormat = Some(int)))
      case 514 => tags.copy(jpegTags = tags.jpegTags.copy(
        jpegInterchangeFormatLength = Some(int)))
    }
  }

  private def readIntArrayTag(streamArray: Array[Char], metadata: TagMetadata,
    tags: IFDTags) = {
    val ints = metadata.length match {
      case 1 => Array(metadata.offset)
      case _ => getIntDataArray(streamArray, metadata)
    }

    metadata.tag match {
      case 273 => tags.copy(basics = tags.basics.copy(stripOffsets =
        Some(ints)))
      case 279 => tags.copy(basics = tags.basics.copy(stripByteCounts =
        Some(ints)))
      case 288 => tags.copy(nonBasics = tags.nonBasics.copy(freeOffsets =
        Some(ints)))
      case 289 => tags.copy(nonBasics = tags.nonBasics.copy(freeByteCounts
          = Some(ints)))
      case 324 => tags.copy(tilesTags = tags.tilesTags.copy(tileOffsets
          = Some(ints)))
      case 325 => tags.copy(tilesTags = tags.tilesTags.copy(tileOffsets
          = Some(ints)))
      case 519 => tags.copy(jpegTags = tags.jpegTags.copy(jpegQTables =
        Some(ints)))
      case 520 => tags.copy(jpegTags = tags.jpegTags.copy(jpegDCTables =
        Some(ints)))
      case 521 => tags.copy(jpegTags = tags.jpegTags.copy(jpegACTables =
        Some(ints)))
      case 532 => tags.copy(colimetryTags = tags.colimetryTags.copy(
        referenceBlackWhite = Some(ints)))
    }
  }

  private def readFractionalTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val fractional = getFractional(streamArray)(metadata.offset)

    metadata.tag match {
      case 282 => tags.copy(basics = tags.basics.copy(xResolution =
        Some(fractional)))
      case 283 => tags.copy(basics = tags.basics.copy(yResolution =
        Some(fractional)))
    }
  }

  private def readFractionalArrayTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val fractionals = getFractionalDataArray(streamArray, metadata)

    metadata.tag match {
      case 286 => tags.copy(documentationTags = tags.documentationTags.copy(
        xPositions = Some(fractionals)))
      case 287 => tags.copy(documentationTags = tags.documentationTags.copy(
        yPositions = Some(fractionals)))
      case 318 => tags.copy(colimetryTags = tags.colimetryTags.copy(
        whitePoints = Some(fractionals)))
      case 319 => tags.copy(colimetryTags = tags.colimetryTags.copy(
        primaryChromaticities = Some(fractionals)))
      case 529 => tags.copy(yCbCrTags = tags.yCbCrTags.copy(
        yCbCrCoefficients = Some(fractionals)))
    }
  }

  private def readDoubleArrayTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val doubleArray = getDoubleDataArray(streamArray, metadata)

    metadata.tag match {
      case 34736 => tags.copy(geoTiffTags = tags.geoTiffTags.copy(doubles =
        Some(doubleArray)))
    }
  }

  private def readModelPixelScaleTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val getDoubleValue = getDouble(streamArray)(_)

    val scaleX = getDoubleValue(metadata.offset)
    val scaleY = getDoubleValue(metadata.offset + 8)
    val scaleZ = getDoubleValue(metadata.offset + 16)

    tags.copy(geoTiffTags = tags.geoTiffTags.copy(modelPixelScale =
      Some((scaleX, scaleY, scaleZ))))
  }

  private def readModelTiePointsTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val numberOfPoints = metadata.length / 6
    val points = Array.ofDim[ModelTiePoint](numberOfPoints)

    val getDoubleValue = getDouble(streamArray)(_)

    for (index <- 0 until numberOfPoints) {
      val current = metadata.offset + index * 6 * 8

      val i = getDoubleValue(current)
      val j = getDoubleValue(current + 8)
      val k = getDoubleValue(current + 16)
      val x = getDoubleValue(current + 24)
      val y = getDoubleValue(current + 32)
      val z = getDoubleValue(current + 40)

      points(index) = ModelTiePoint(i, j, k, x, y, z)
    }

    tags.copy(geoTiffTags = tags.geoTiffTags.copy(modelTiePoints =
      Some(points)))
  }

  private def readGeoKeyDirectoryTag(streamArray: Array[Char],
    metadata: TagMetadata, tags: IFDTags) = {
    val getShortValue = getShort(streamArray)(_)

    val version = getShortValue(metadata.offset)
    val keyRevision = getShortValue(metadata.offset + 2)
    val minorRevision = getShortValue(metadata.offset + 4)
    val numberOfKeys = getShortValue(metadata.offset + 6)

    val directoryMetadata = KeyDirectoryMetadata(version, keyRevision,
      minorRevision, numberOfKeys)

    val directoryReader = KeyDirectoryReader(tags.geoTiffTags.asciis.get,
      tags.geoTiffTags.doubles.get)

    val directory = directoryReader.read(streamArray, metadata.offset + 8,
      GeoKeyDirectory(count = numberOfKeys), 0)

    tags.copy(geoTiffTags = tags.geoTiffTags.copy(geoKeyDirectory =
      Some(directory)))
  }

}
