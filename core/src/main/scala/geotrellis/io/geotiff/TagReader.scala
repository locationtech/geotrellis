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

import geotrellis.io.geotiff.utils.ByteBufferUtils._

case class TagReader(byteBuffer: ByteBuffer) {

  def read(directory: ImageDirectory, tagMetadata: TagMetadata):
      ImageDirectory = (tagMetadata.tag, tagMetadata.fieldType,
        tagMetadata.length) match {
    case (33550, _, _) => readModelPixelScaleTag(directory, tagMetadata)
    case (33922, _, _) => readModelTiePointsTag(directory, tagMetadata)
    case (34735, _, _) => readGeoKeyDirectoryTag(directory, tagMetadata)
    case (_, 1, _) => readByteVectorTag(directory, tagMetadata)
    case (_, 2, _) => readAsciiVectorTag(directory, tagMetadata)
    case (_, 3, 1) => readShortTag(directory, tagMetadata)
    case (_, 3, _) => readShortVectorTag(directory, tagMetadata)
    case (_, 4, 1) => readIntTag(directory, tagMetadata)
    case (_, 4, _) => readIntVectorTag(directory, tagMetadata)
    case (_, 5, 1) => readFractionalTag(directory, tagMetadata)
    case (_, 5, _) => readFractionalVectorTag(directory, tagMetadata)
    case (_, 12, _) => readDoubleVectorTag(directory, tagMetadata)
  }

  private def readModelPixelScaleTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val offset = byteBuffer.position
    byteBuffer.position(tagMetadata.offset)

    val scaleX = byteBuffer.getDouble
    val scaleY = byteBuffer.getDouble
    val scaleZ = byteBuffer.getDouble

    byteBuffer.position(offset)

    directory.copy(geoTiffTags = directory.geoTiffTags.copy(modelPixelScale =
      Some((scaleX, scaleY, scaleZ))))
  }

  private def readModelTiePointsTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val numberOfPoints = tagMetadata.length / 6

    val offset = byteBuffer.position
    byteBuffer.position(tagMetadata.offset)

    val points = (for (i <- 0 until numberOfPoints) yield ModelTiePoint(
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble,
      byteBuffer.getDouble
    )).toVector

    byteBuffer.position(offset)

    directory.copy(geoTiffTags =
      directory.geoTiffTags.copy(modelTiePoints = Some(points)))
  }

  private def readGeoKeyDirectoryTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {

    val offset = byteBuffer.position

    byteBuffer.position(tagMetadata.offset)

    val version = byteBuffer.getShort
    val keyRevision = byteBuffer.getShort
    val minorRevision = byteBuffer.getShort
    val numberOfKeys = byteBuffer.getShort

    val keyDirectoryMetadata = KeyDirectoryMetadata(version, keyRevision,
      minorRevision, numberOfKeys)

    val keyDirectoryReader = KeyDirectoryReader(byteBuffer, directory)

    val geoKeyDirectory = keyDirectoryReader.read(GeoKeyDirectory(count =
      numberOfKeys))

    byteBuffer.position(offset)

    directory.copy(geoTiffTags = directory.geoTiffTags.copy(geoKeyDirectory =
      Some(geoKeyDirectory)))
  }

  private def readByteVectorTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val bytes = byteBuffer.getByteVector(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case 336 => directory.copy(cmykTags = directory.cmykTags.copy(dotRange =
        Some(bytes)))
      case 338 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(extraSamples = Some(bytes)))
    }
  }

  private def readAsciiVectorTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val string = byteBuffer.getString(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case 270 => directory.copy(metadataTags =
        directory.metadataTags.copy(imageDesc = Some(string)))
      case 271 => directory.copy(metadataTags =
        directory.metadataTags.copy(maker = Some(string)))
      case 272 => directory.copy(metadataTags =
        directory.metadataTags.copy(model = Some(string)))
      case 305 => directory.copy(metadataTags =
        directory.metadataTags.copy(software = Some(string)))
      case 315 => directory.copy(metadataTags =
        directory.metadataTags.copy(artist = Some(string)))
      case 316 => directory.copy(metadataTags =
        directory.metadataTags.copy(computer = Some(string)))
      case 33432 => directory.copy(metadataTags =
        directory.metadataTags.copy(copyright = Some(string)))
      case 34737 => directory.copy(geoTiffTags =
        directory.geoTiffTags.copy(asciis = Some(string)))
    }
  }

  private def readShortTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val short = byteBuffer.getShortFromInt(tagMetadata.offset)

    tagMetadata.tag match {
      case 255 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(subfileType = Some(short)))
      case 256 => directory.copy(basicTags =
        directory.basicTags.copy(imageWidth = Some(short)))
      case 257 => directory.copy(basicTags =
        directory.basicTags.copy(imageLength = Some(short)))
      case 258 => directory.copy(basicTags =
        directory.basicTags.copy(bitsPerSample = Vector(short)))
      case 259 => directory.copy(basicTags =
        directory.basicTags.copy(compression = short))
      case 262 => directory.copy(basicTags = directory.basicTags.copy(
        photometricInterp = Some(short)))
      case 263 => directory.copy(nonBasicTags = directory.nonBasicTags.copy(
        thresholding = short))
      case 264 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(cellWidth = Some(short)))
      case 265 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(cellLength = Some(short)))
      case 266 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(fillOrder = Some(short)))
      case 274 => directory.copy(nonBasicTags = directory.nonBasicTags.copy(
        orientation = Some(short)))
      case 277 => directory.copy(basicTags = directory.basicTags.copy(
        samplesPerPixel = short))
      case 278 => directory.copy(basicTags = directory.basicTags.copy(
        rowsPerStrip = short))
      case 284 => directory.copy(nonBasicTags = directory.nonBasicTags.copy(
        planarConfiguration
          = Some(short)))
      case 290 => directory.copy(nonBasicTags = directory.nonBasicTags.copy(
        grayResponseUnit
          = Some(short)))
      case 296 => directory.copy(basicTags =
        directory.basicTags.copy(resolutionUnit = Some(short)))
      case 317 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(predictor = Some(short)))
      case 322 => directory.copy(tileTags = directory.tileTags.copy(
        tileWidth = Some(short)))
      case 323 => directory.copy(tileTags = directory.tileTags.copy(
        tileLength = Some(short)))
      case 332 => directory.copy(cmykTags = directory.cmykTags.copy(
        inkSet = Some(short)))
      case 334 => directory.copy(cmykTags = directory.cmykTags.copy(
        numberOfInks = Some(short)))
      case 512 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegProc = Some(short)))
      case 513 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegInterchangeFormat = Some(short)))
      case 514 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegInterchangeFormatLength = Some(short)))
      case 515 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegRestartInterval = Some(short)))
      case 531 => directory.copy(yCbCrTags = directory.yCbCrTags.copy(
        yCbCrPositioning = Some(short)))
    }
  }

  private def readShortVectorTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val shorts = byteBuffer.getShortVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case 258 => directory.copy(basicTags = directory.basicTags.copy(
        bitsPerSample = shorts))
      case 273 => directory.copy(basicTags = directory.basicTags.copy(
        stripOffsets = Some(shorts)))
      case 279 => directory.copy(basicTags = directory.basicTags.copy(
        stripByteCounts = Some(shorts)))
      case 280 => directory.copy(dataSampleFormatTags =
        directory.dataSampleFormatTags.copy(minSampleValues = Some(shorts)))
      case 281 => directory.copy(dataSampleFormatTags =
        directory.dataSampleFormatTags.copy(maxSampleValues = Some(shorts)))
      case 291 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(grayResponseCurve = Some(shorts)))
      case 297 => directory.copy(documentationTags =
        directory.documentationTags.copy(pageNumbers = Some(shorts)))
      case 301 => directory.copy(colimetryTags = directory.colimetryTags.copy(
        transferFunction = Some(shorts)))
      case 320 => directory.copy(basicTags = directory.basicTags.copy(colorMap =
        Some(shorts)))
      case 321 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(halftoneHints = Some(shorts)))
      case 325 => directory.copy(tileTags = directory.tileTags.copy(
        tileByteCounts = Some(shorts)))
      case 336 => directory.copy(cmykTags = directory.cmykTags.copy(dotRange =
        Some(shorts)))
      case 339 => directory.copy(dataSampleFormatTags =
        directory.dataSampleFormatTags.copy(sampleFormat = Some(shorts)))
      case 342 => directory.copy(colimetryTags = directory.colimetryTags.copy(
        transferRange = Some(shorts)))
      case 517 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegLosslessPredictors = Some(shorts)))
      case 518 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegPointTransforms = Some(shorts)))
    }
  }

  private def readIntTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val int = tagMetadata.offset

    tagMetadata.tag match {
      case 254 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(newSubfileType = Some(int)))
      case 256 => directory.copy(basicTags = directory.basicTags.copy(
        imageWidth = Some(int)))
      case 257 => directory.copy(basicTags = directory.basicTags.copy(
        imageLength = Some(int)))
      case 292 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(t4Options = Some(int)))
      case 293 => directory.copy(nonBasicTags =
        directory.nonBasicTags.copy(t6Options = Some(int)))
      case 322 => directory.copy(tileTags = directory.tileTags.copy(tileWidth
          = Some(int)))
      case 323 => directory.copy(tileTags = directory.tileTags.copy(tileLength
          = Some(int)))
      case 513 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegInterchangeFormat = Some(int)))
      case 514 => directory.copy(jpegTags = directory.jpegTags.copy(
        jpegInterchangeFormatLength = Some(int)))
    }
  }

  private def readIntVectorTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val ints = byteBuffer.getIntVector(tagMetadata.length, tagMetadata.offset)

    tagMetadata.tag match {
      case 273 => directory.copy(basicTags = directory.basicTags.copy(
        stripOffsets = Some(ints)))
      case 279 => directory.copy(basicTags = directory.basicTags.copy(
        stripByteCounts = Some(ints)))
      case 288 => directory.copy(nonBasicTags = directory.nonBasicTags.copy(
        freeOffsets = Some(ints)))
      case 289 => directory.copy(nonBasicTags = directory.nonBasicTags.copy(
        freeByteCounts = Some(ints)))
      case 324 => directory.copy(tileTags = directory.tileTags.copy(tileOffsets
          = Some(ints)))
      case 325 => directory.copy(tileTags = directory.tileTags.copy(tileOffsets
          = Some(ints)))
      case 519 => directory.copy(jpegTags = directory.jpegTags.copy(jpegQTables
          = Some(ints)))
      case 520 => directory.copy(jpegTags = directory.jpegTags.copy(jpegDCTables
          = Some(ints)))
      case 521 => directory.copy(jpegTags = directory.jpegTags.copy(jpegACTables
          = Some(ints)))
      case 532 => directory.copy(colimetryTags = directory.colimetryTags.copy(
        referenceBlackWhite = Some(ints)))
    }
  }

  private def readFractionalTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractional = byteBuffer.getFractionalVector(tagMetadata.length,
      tagMetadata.offset)(0)

    tagMetadata.tag match {
      case 282 => directory.copy(basicTags = directory.basicTags.copy(
        xResolution = Some(fractional)))
      case 283 => directory.copy(basicTags = directory.basicTags.copy(
        yResolution = Some(fractional)))
    }
  }

  private def readFractionalVectorTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val fractionals = byteBuffer.getFractionalVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case 286 => directory.copy(documentationTags =
        directory.documentationTags.copy(xPositions = Some(fractionals)))
      case 287 => directory.copy(documentationTags =
        directory.documentationTags.copy(yPositions = Some(fractionals)))
      case 318 => directory.copy(colimetryTags = directory.colimetryTags.copy(
        whitePoints = Some(fractionals)))
      case 319 => directory.copy(colimetryTags = directory.colimetryTags.copy(
        primaryChromaticities = Some(fractionals)))
      case 529 => directory.copy(yCbCrTags = directory.yCbCrTags.copy(
        yCbCrCoefficients = Some(fractionals)))
    }
  }

  private def readDoubleVectorTag(directory: ImageDirectory,
    tagMetadata: TagMetadata) = {
    val doubles = byteBuffer.getDoubleVector(tagMetadata.length,
      tagMetadata.offset)

    tagMetadata.tag match {
      case 34736 => directory.copy(geoTiffTags = directory.geoTiffTags.copy(
        doubles = Some(doubles)))
    }
  }

}
