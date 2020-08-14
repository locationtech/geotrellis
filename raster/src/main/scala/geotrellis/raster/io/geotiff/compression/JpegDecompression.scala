/*
 * Copyright 2018 Azavea
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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags.{TiffTags, JpegTags, YCbCrTags, ColimetryTags}
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace
import geotrellis.raster.io.geotiff.tags.codes.CompressionType._

import spire.syntax.cfor._

import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import javax.imageio.{ImageIO, IIOException, ImageReader}
import javax.imageio.plugins.jpeg.JPEGImageReadParam

object JpegDecompressor {
  def apply(tiffTags: TiffTags): JpegDecompressor =
    new JpegDecompressor(tiffTags)
}

class JpegDecompressor(tiffTags: TiffTags) extends Decompressor {
  val jpegTags: JpegTags = tiffTags.jpegTags
  val yCbCrTags: YCbCrTags = tiffTags.yCbCrTags
  val colimetryTags: ColimetryTags = tiffTags.colimetryTags

  val photometricInterp = tiffTags.basicTags.photometricInterp

  final val CCIR_601_1_COEFFICIENTS = Array(299.0 / 1000.0, 587.0 / 1000.0, 114.0 / 1000.0)
  final val REFERENCE_BLACK_WHITE_YCC_DEFAULT = Array[Double](0, 255, 128, 255, 128, 255)

  val jpegTables =
    jpegTags.jpegTables.getOrElse {
      val msg = "compression type JPEG is not supported by this reader if JPEG Tables tag is not set."
      throw new GeoTiffReaderLimitationException(msg)
    }

  private def withJpegReader[T](inputBytes: Option[Array[Byte]])(fn: ImageReader => T): T = {
    val readers = ImageIO.getImageReadersByFormatName("JPEG")
    if(!readers.hasNext) {
      throw new IIOException("Could not instantiate JPEGImageReader")
    }
    val reader = readers.next
    val tablesSource = ImageIO.createImageInputStream(new ByteArrayInputStream(jpegTables))
    val imageSource = inputBytes.map { ib =>
      ImageIO.createImageInputStream(new ByteArrayInputStream(ib))
    }

    try {
      // This initializes the tables and other internal settings for the reader,
      // and is actually a feature of JPEG, see abbreviated streams:
      // http://docs.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html#abbrev
      reader.setInput(tablesSource)
      reader.getStreamMetadata()

      imageSource.foreach(reader.setInput(_))
      fn(reader)

    } finally {
      tablesSource.close()
      imageSource.foreach(_.close())
      reader.dispose()
    }
  }

  val jpegParam = withJpegReader(None) { jpegReader =>
    val params = jpegReader.getDefaultReadParam.asInstanceOf[JPEGImageReadParam]
    params.setSourceSubsampling(1, 1, 0, 0)
    params
  }

  def  normalizeYCbCr(array: Array[Byte]): Array[Byte] = {
    // Default:  CCIR Recommendation 601-1: 299/1000, 587/1000 and 114/1000
    val coefficients =
      yCbCrTags.yCbCrCoefficients.map { rationals =>
        rationals.map { case (a, b) => a / b.toDouble }
      }.getOrElse(CCIR_601_1_COEFFICIENTS)

    val referenceBW =
      colimetryTags.referenceBlackWhite.
        map { v => v.map(_.toDouble) }.
        getOrElse(REFERENCE_BLACK_WHITE_YCC_DEFAULT)

    cfor(0)(_ < array.length, _ + 3) { i =>
      YCbCrConverter.convertYCbCr2RGB(array, array, coefficients, referenceBW, i);
    }

    array
  }

  def code = JpegCoded

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val result = withJpegReader(Some(segment)) { jpegReader =>
      jpegReader.readRaster(0, jpegParam).getDataBuffer.asInstanceOf[DataBufferByte].getData
    }

    if(photometricInterp == ColorSpace.YCbCr)
      normalizeYCbCr(result)
    else
      result
  }
}

/**
 * Fast YCbCr to RGB conversion. Ported from Twelvemonkeys
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author Original code by Werner Randelshofer (used by permission).

BSD 3-Clause License

Copyright (c) 2017, Harald Kuhr
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
object YCbCrConverter {
  /**
    * Define tables for YCC->RGB color space conversion.
    */
  private final val SCALEBITS = 16
  private final val MAXJSAMPLE = 255
  private final val CENTERJSAMPLE = 128
  private final val ONE_HALF = 1 << (SCALEBITS - 1)

  private final val Cr_R_LUT = Array.ofDim[Int](MAXJSAMPLE + 1)
  private final val Cb_B_LUT = Array.ofDim[Int](MAXJSAMPLE + 1)
  private final val Cr_G_LUT = Array.ofDim[Int](MAXJSAMPLE + 1)
  private final val Cb_G_LUT = Array.ofDim[Int](MAXJSAMPLE + 1)

    /**
     * Initializes tables for YCC->RGB color space conversion.
     */
    private def buildYCCtoRGBtable() {
      var i = 0
      var x = -CENTERJSAMPLE
      while(i <= MAXJSAMPLE) {
        // i is the actual input pixel value, in the range 0..MAXJSAMPLE
        // The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE
        // Cr=>R value is nearest int to 1.40200 * x
        Cr_R_LUT(i) = ((1.40200 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF).toInt >> SCALEBITS
        // Cb=>B value is nearest int to 1.77200 * x
        Cb_B_LUT(i) = ((1.77200 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF).toInt >> SCALEBITS
        // Cr=>G value is scaled-up -0.71414 * x
        Cr_G_LUT(i) = -((0.71414 * (1 << SCALEBITS) + 0.5) * x).toInt
        // Cb=>G value is scaled-up -0.34414 * x
        // We also add in ONE_HALF so that need not do it in inner loop
        Cb_G_LUT(i) = -(((0.34414) * (1 << SCALEBITS) + 0.5) * x + ONE_HALF).toInt

        i += 1
        x += 1
      }
    }

  buildYCCtoRGBtable()

  def convertYCbCr2RGB(
    yCbCr: Array[Byte],
    rgb: Array[Byte],
    coefficients: Array[Double],
    referenceBW: Array[Double],
    offset: Int
  ) = {
    var y = 0.0
    var cb = 0.0
    var cr = 0.0

    if (referenceBW == null) {
      // Default case
      y = (yCbCr(offset) & 0xff)
      cb = (yCbCr(offset + 1) & 0xff) - 128
      cr = (yCbCr(offset + 2) & 0xff) - 128
    }
    else {
      // Custom values
      y = ((yCbCr(offset) & 0xff) - referenceBW(0)) * 255.0 / (referenceBW(1) - referenceBW(0))
      cb = ((yCbCr(offset + 1) & 0xff) - referenceBW(2)) * 127.0 / (referenceBW(3) - referenceBW(2))
      cr = ((yCbCr(offset + 2) & 0xff) - referenceBW(4)) * 127.0 / (referenceBW(5) - referenceBW(4))
    }

    val lumaRed = coefficients(0)
    val lumaGreen = coefficients(1)
    val lumaBlue = coefficients(2)

    val red = Math.round(cr * (2.0 - 2.0 * lumaRed) + y).toInt
    val blue =  Math.round(cb * (2.0 - 2.0 * lumaBlue) + y).toInt
    val green = Math.round((y - lumaRed * red - lumaBlue * blue) / lumaGreen).toInt

    rgb(offset) = clamp(red)
    rgb(offset + 2) = clamp(blue)
    rgb(offset + 1) = clamp(green)
  }

  def convertYCbCr2RGB(yCbCr: Array[Byte], rgb: Array[Byte], offset: Int) = {
    val y = yCbCr(offset) & 0xff
    val cr = yCbCr(offset + 2) & 0xff
    val cb = yCbCr(offset + 1) & 0xff

    rgb(offset) = clamp(y + Cr_R_LUT(cr))
    rgb(offset + 1) = clamp(y + (Cb_G_LUT(cb) + Cr_G_LUT(cr) >> SCALEBITS))
    rgb(offset + 2) = clamp(y + Cb_B_LUT(cb))
  }

  def clamp(v: Int): Byte =
    Math.max(0, Math.min(255, v)).toByte
}
