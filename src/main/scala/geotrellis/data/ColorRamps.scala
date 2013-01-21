package geotrellis.data

import scala.math.round

import geotrellis._

/**
 * *
 * List of colors for rendering a raster.
 *
 * In order to select colors for a raster, one often divides the values in
 * the raster into distinct ranges and selects a color for each range.  A
 * ColorRamp provides the colors from which to select.
 *
 * NB: The list of colors are RGBA values, which are usually represented
 *     as 8 character hex string, like "0x0000FFFF" where the last two
 *     character encode an alpha (transparency/opacity) value.
 *
 *     Usually on the web, you see "RGB" or "hex" color values,
 *     like 0x0000FF (blue).  If you have a list of RGB values,
 *     use the method ColorRamp.createWithRGBColors.
 *
 * @param colors  An array of RGBA integer values
 */
class ColorRamp(val colors: Array[Int]) {
  /**
   * Generate n new colors using the existing color ramp as a guide.
   *
   * The first and last colors of the existing ramp will be preserved in the output
   * ColorRamp.  For example, given a color ramp of two colors, red and yellow, a request
   * for 5 colors would return Red, Yellowish-Red, Orange, Reddish-Yellow, Yellow.
   */
  def interpolate(nBreaks: Int): ColorRamp = ???
  def alphaGradient(start: Int = 0, stop: Int = 0xFF): ColorRamp = ???
  def setAlpha(a: Int): ColorRamp = ???
}

object ColorRamp {
  def apply(colors: Int*) = new ColorRamp(colors.toArray)

  /**
   * Create a color camp with an array of RGB colors (e.g. 0xFF0000 for red).
   */
  def createWithRGBColors(rgbColors: Int*): ColorRamp = ???

}

object ColorRamps {

  /** Blue to orange color ramp. */
  val BlueToOrange = ColorRamp.createWithRGBColors(
    0x2586AB, 0x4EA3C8, 0x7FB8D4, 0xADD8EA,
    0xC8E1E7, 0xEDECEA, 0xF0E7BB, 0xF5CF7D,
    0xF9B737, 0xE68F2D, 0xD76B27)

  /** Light yellow to orange color ramp. */
  val LightYellowToOrange = ColorRamp.createWithRGBColors(
    0x118C8C, 0x429D91, 0x61AF96, 0x75C59B,
    0xA2CF9F, 0xC5DAA3, 0xE6E5A7, 0xE3D28F,
    0xE0C078, 0xDDAD62, 0xD29953, 0xCA8746,
    0xC2773B)

  /** Blue to red color ramp. */
  val BlueToRed = ColorRamp.createWithRGBColors(
    0x2791C3, 0x5DA1CA, 0x83B2D1, 0xA8C5D8,
    0xCCDBE0, 0xE9D3C1, 0xDCAD92, 0xD08B6C,
    0xC66E4B, 0xBD4E2E)

  /** Green to red-orange color ramp. */
  val GreenToRedOrange = ColorRamp.createWithRGBColors(
    0x569543, 0x9EBD4D, 0xBBCA7A, 0xD9E2B2,
    0xE4E7C4, 0xE6D6BE, 0xE3C193, 0xDFAC6C,
    0xDB9842, 0xB96230)

  /** Light to dark (sunset) color ramp. */
  val LightToDarkSunset = ColorRamp.createWithRGBColors(
    0xFFFFFF, 0xFBEDD1, 0xF7E0A9, 0xEFD299,
    0xE8C58B, 0xE0B97E, 0xF2924D, 0xC97877,
    0x946196, 0x2AB7D6, 0x474040)

  /** Light to dark (green) color ramp. */
  val LightToDarkGreen = ColorRamp.createWithRGBColors(
    0xE8EDDB, 0xDCE8D4, 0xBEDBAD, 0xA0CF88,
    0x81C561, 0x4BAF48, 0x1CA049, 0x3A6D35)

  /** Yellow to red color ramp, 0xfor use in heatmaps. */
  val HeatmapYellowToRed = ColorRamp.createWithRGBColors(
    0xF7DA22, 0xECBE1D, 0xE77124, 0xD54927,
    0xCF3A27, 0xA33936, 0x7F182A, 0x68101A)

  /** Blue to yellow to red spectrum color ramp, 0xfor use in heatmaps. */
  val HeatmapBlueToYellowToRedSpectrum = ColorRamp.createWithRGBColors(
    0x2A2E7F, 0x3D5AA9, 0x4698D3, 0x39C6F0,
    0x76C9B3, 0xA8D050, 0xF6EB14, 0xFCB017,
    0xF16022, 0xEE2C24, 0x7D1416)

  /** Dark red to yellow white color ramp, 0xfor use in heatmaps. */
  val HeatmapDarkRedToYellowWhite = ColorRamp.createWithRGBColors(
    0x68101A, 0x7F182A, 0xA33936, 0xCF3A27,
    0xD54927, 0xE77124, 0xECBE1D, 0xF7DA22,
    0xF6EDB1, 0xFFFFFF)

  /** Light purple to dark purple to white, 0xfor use in heatmaps. */
  val HeatmapLightPurpleToDarkPurpleToWhite = ColorRamp.createWithRGBColors(
    0xA52278, 0x993086, 0x8C3C97, 0x6D328A,
    0x4E2B81, 0x3B264B, 0x180B11, 0xFFFFFF)

  /** Bold Land Use color ramp, 0xfor use in land use classification. */
  val ClassificationBoldLandUse = ColorRamp.createWithRGBColors(
    0xB29CC3, 0x4F8EBB, 0x8F9238, 0xC18437,
    0xB5D6B1, 0xD378A6, 0xD4563C, 0xF9BE47)

  /** Muted terrain color ramp, 0xfor use in classification. */
  val ClassificationMutedTerrain = ColorRamp.createWithRGBColors(
    0xCEE1E8, 0x7CBCB5, 0x82B36D, 0x94C279,
    0xD1DE8D, 0xEDECC3, 0xCCAFB4, 0xC99884)
}
