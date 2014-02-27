package geotrellis.render

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
class ColorRamp(val colors: Seq[Int]) {
  /**
   * Generate n new colors using the existing color ramp as a guide.
   *
   * The first and last colors of the existing ramp will be preserved in the output
   * ColorRamp.  For example, given a color ramp of two colors, red and yellow, a request
   * for 5 colors would return Red, Yellowish-Red, Orange, Reddish-Yellow, Yellow.
   */
  def interpolate(nBreaks: Int): ColorRamp = 
    ColorRamp(Color.chooseColors(colors.toArray, nBreaks))

  def alphaGradient(start: Int = 0, stop: Int = 0xFF): ColorRamp = {
    val alphas = Color.chooseColors(start,stop, colors.length).map(Color.unzipA)
//.getRanges(Color.unzipA, colors.length)
    
    val newColors = colors.zip(alphas).map ({ case (color, a) => 
      val (r,g,b,_) = Color.unzip(color)
      Color.zip(r,g,b,a)
    })

    ColorRamp(newColors) 
  }
  def setAlpha(a: Int): ColorRamp = {
    val newColors = colors.map { color => 
      val(r,g,b,_) = Color.unzip(color)
      Color.zip(r,g,b,a)
    }
    ColorRamp(newColors)
  }  
  def toArray = colors.toArray
}

object ColorRamp {
  def apply(colors:Seq[Int]):ColorRamp = new ColorRamp(colors)

  /**
   * Create a color camp with an array of RGB colors (e.g. 0xFF0000 for red).
   */
  def createWithRGBColors(rgbColors: Int*): ColorRamp = 
    new ColorRamp(rgbColors.map(Color.rgbToRgba(_)))

}
