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

import scala.util.Sorting

object GeoTiffIFD {

  def apply(fields: Array[GeoTiffField]) = new GeoTiffIFD(fields)

}

case class GeoTiffIFDBuilder(
  imageWidth: Option[Int],
  imageHeight: Option[Int],
  compression: Option[Int],
  photometricInterp: Option[Int],
  stripOffsets: Option[Array[Int]],
  rowsPerStrip: Option[Int],
  stripByteCounts: Option[Array[Int]],
  xResolution: Option[Double],
  yResolution: Option[Double],
  resolutionUnit: Option[Int],
  bitsPerSample: Option[Int],
  colorMap: Option[Array[Int]],
  samplesPerPixel: Option[Int]
) {

  def add(fieldImageWidth: GTFieldImageWidth) = GeoTiffIFDBuilder(
    Some(fieldImageWidth.width), imageHeight, compression,
    photometricInterp, stripOffsets, rowsPerStrip, stripByteCounts,
    xResolution, yResolution, resolutionUnit, bitsPerSample, colorMap,
    samplesPerPixel)

  def add(fieldImageHeight: GTFieldImageHeight) = GeoTiffIFDBuilder(imageWidth,
    Some(fieldImageHeight.height), compression, photometricInterp, stripOffsets,
    rowsPerStrip, stripByteCounts, xResolution, yResolution, resolutionUnit,
    bitsPerSample, colorMap, samplesPerPixel)

  def add(fieldCompression: GTFieldCompression) = GeoTiffIFDBuilder(imageWidth,
    imageHeight, Some(fieldCompression.compression), photometricInterp,
    stripOffsets, rowsPerStrip, stripByteCounts, xResolution,
    yResolution, resolutionUnit, bitsPerSample, colorMap, samplesPerPixel)

  def add(fieldPhotometricInterp: GTFieldPhotometricInterpretation) =
    GeoTiffIFDBuilder(imageWidth, imageHeight,
      compression, Some(fieldPhotometricInterp.photometricInterp), stripOffsets,
      rowsPerStrip, stripByteCounts, xResolution,
      yResolution, resolutionUnit, bitsPerSample, colorMap,
      samplesPerPixel)

  def add(fieldStripOffsets: GTFieldStripOffsets) = GeoTiffIFDBuilder(
    imageWidth, imageHeight, compression, photometricInterp,
    Some(fieldStripOffsets.stripOffsets), rowsPerStrip, stripByteCounts,
    xResolution, yResolution, resolutionUnit, bitsPerSample, colorMap,
    samplesPerPixel)

  def add(fieldRowsPerStrip: GTFieldRowsPerStrip) = GeoTiffIFDBuilder(
    imageWidth, imageHeight, compression, photometricInterp, stripOffsets,
    Some(fieldRowsPerStrip.rowsPerStrip), stripByteCounts, xResolution,
    yResolution, resolutionUnit, bitsPerSample, colorMap, samplesPerPixel)

  def add(fieldStripByteCounts: GTFieldStripByteCounts) = GeoTiffIFDBuilder(
    imageWidth, imageHeight, compression, photometricInterp, stripOffsets,
    rowsPerStrip, Some(fieldStripByteCounts.stripByteCounts), xResolution,
    yResolution, resolutionUnit, bitsPerSample, colorMap, samplesPerPixel)

  def add(fieldXResolution: GTFieldXResolution) = GeoTiffIFDBuilder(imageWidth,
    imageHeight, compression, photometricInterp, stripOffsets,
    rowsPerStrip, stripByteCounts, Some(fieldXResolution.xResolution),
    yResolution, resolutionUnit, bitsPerSample, colorMap, samplesPerPixel)

  def add(fieldYResolution: GTFieldYResolution) = GeoTiffIFDBuilder(imageWidth,
    imageHeight, compression, photometricInterp, stripOffsets,
    rowsPerStrip, stripByteCounts, xResolution,
    Some(fieldYResolution.yResolution), resolutionUnit, bitsPerSample, colorMap,
    samplesPerPixel)

  def add(fieldResolutionUnit: GTFieldResolutionUnit) = GeoTiffIFDBuilder(
    imageWidth, imageHeight, compression, photometricInterp, stripOffsets,
    rowsPerStrip, stripByteCounts, xResolution, yResolution,
    Some(fieldResolutionUnit.resolutionUnit), bitsPerSample, colorMap,
    samplesPerPixel)

  def add(fieldBitsPerSample: GTFieldBitsPerSample) = GeoTiffIFDBuilder(
    imageWidth, imageHeight, compression, photometricInterp, stripOffsets,
    rowsPerStrip, stripByteCounts, xResolution, yResolution,
    resolutionUnit, Some(fieldBitsPerSample(bitsPerSample), colorMap,
    samplesPerPixel)

  def add(fieldColorMap: GTFieldColorMap) = GeoTiffIFDBuilder(imageWidth,
    imageHeight, compression, photometricInterp, stripOffsets,
    rowsPerStrip, stripByteCounts, xResolution, yResolution,
    resolutionUnit, bitsPerSample, Some(fieldColorMap.colorMap),
    samplesPerPixel)

  def add(fieldSamplesPerPixel: GTFieldSamplesPerPixel) =
    GeoTiffIFDBuilder(imageWidth, imageHeight, compression, photometricInterp,
      stripOffsets, rowsPerStrip, stripByteCounts, xResolution, yResolution,
    resolutionUnit, bitsPerSample, colorMap,
      Some(fieldSamplesPerPixel.samplesPerPixel))

  def build =

  def setDefaults = {
    GeoTiffIFDBuilder(imageWidth, imageHeight, compression,)
  }

}

trait GeoTiffIFD

case class GTBilevelIFD(
  imageWidth: Int,
  imageHeight: Int,
  compression: Int,
  photometricInterp: Int,
  stripOffsets: Array[Int],
  rowsPerStrip: Int,
  stripByteCounts: Array[Int],
  xResolution: Double,
  yResolution: Double,
  resolutionUnit: Int
) extends GeoTiffIFD

case class GTGrayScaleIFD(
  imageWidth: Int,
  imageHeight: Int,
  compression: Int,
  photometricInterp: Int,
  stripOffsets: Array[Int],
  rowsPerStrip: Int,
  stripByteCounts: Array[Int],
  xResolution: Double,
  yResolution: Double,
  resolutionUnit: Int,
  bitsPerSample: Int
) extends GeoTiffIFD

case class GTPaletteColorIFD(
  imageWidth: Int,
  imageHeight: Int,
  compression: Int,
  photometricInterp: Int,
  stripOffsets: Array[Int],
  rowsPerStrip: Int,
  stripByteCounts: Array[Int],
  xResolution: Double,
  yResolution: Double,
  resolutionUnit: Int,
  bitsPerSample: Int,
  colorMap: Array[Int]
) extends GeoTiffIFD

case class GTRGBIFD(
  imageWidth: Int,
  imageHeight: Int,
  compression: Int,
  photometricInterp: Int,
  stripOffsets: Array[Int],
  rowsPerStrip: Int,
  stripByteCounts: Array[Int],
  xResolution: Double,
  yResolution: Double,
  resolutionUnit: Int,
  bitsPerSample: Int,
  samplesPerPixel: Int
) extends GeoTiffIFD
