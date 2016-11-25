/*
 * Copyright 2016 Azavea
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

package geotrellis.raster.io.geotiff.tags.codes

/**
 * Color space tag values, specifying photometric interpretation of multiple bands via use of
 * [[TagCodes.PhotometricInterpTag]], as defined by
 * http://www.awaresystems.be/imaging/tiff/tifftags/photometricinterpretation.html
 */
object ColorSpace {
  /**
   * For bilevel and grayscale images: 0 is imaged as white.
   */
  val WhiteIsZero = 0
  /**
   * For bilevel and grayscale images: 0 is imaged as black.
   */
  val BlackIsZero = 1
  /**
   * RGB value of (0,0,0) represents black, and (255,255,255) represents white, assuming 8-bit components.
   * The components are stored in the indicated order: first Red, then Green, then Blue.
   */
  val RGB = 2
  /**
   *  Palette color. In this model, a color is described with a single component. The value of the component is
   *  used as an index into the red, green and blue curves in the ColorMap field to retrieve an RGB triplet that
   *  defines the color. When PhotometricInterpretation=3 is used, ColorMap must be present and
   *  SamplesPerPixel must be 1.
   */
  val Palette = 3
  /**
   *  This means that the image is used to define an irregularly shaped region of another image in the same TIFF file.
   *  SamplesPerPixel and BitsPerSample must be 1. PackBits compression is recommended.
   *  The 1-bits define the interior of the region; the 0-bits define the exterior of the region.
   */
  val TransparencyMask = 4
  /** Also called 'separated' */
  val CMYK = 5
  /** YCbCr */
  val YCbCr = 6
  /** CIE L*a*b* */
  val CIELab = 8
  /** ICC L*a*b* */
  val ICCLab = 9
  /**
   * Used in the TIFF-F and TIFF-FX standard (RFC 2301).
   * The Decode tag, if present, holds information about this particular CIE L*a*b* encoding.
   */
  val ITULab = 10
  /**
   *  DNG CFA (Color Filter Array) encoding.
   */
  val CFA = 32803
  /**
   * DNG LinearRaw encoding.
   */
  val LinearRaw = 34892
  /**
   * Pixar 'LogL' encoding.
   */
  val LogL = 32844
  /**
   * Pixar 'LogLuv' encoding.
   */
  val LogLuv = 32845
}
