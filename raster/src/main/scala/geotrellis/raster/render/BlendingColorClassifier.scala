// package geotrellis.raster.render

// import geotrellis.raster.histogram.Histogram

// import scala.collection.mutable
// import scala.reflect.ClassTag

// object BlendingColorClassifier {
//   def apply(breaks: Array[Int], colors: Array[RGBA]): BlendingIntColorClassifier =
//     apply(breaks, colors, None)

//   def apply(breaks: Array[Int], colors: Array[RGBA], noDataColor: Option[RGBA]): BlendingIntColorClassifier = {
//     val colorClassifier = new BlendingIntColorClassifier
//     colorClassifier.addBreaks(breaks).addColors(colors)
//     colorClassifier
//   }

//   def apply(breaks: Array[Double], colors: Array[RGBA]): BlendingDoubleColorClassifier =
//     apply(breaks, colors, None)

//   def apply(breaks: Array[Double], colors: Array[RGBA], noDataColor: Option[RGBA]): BlendingDoubleColorClassifier = {
//     val colorClassifier = new BlendingDoubleColorClassifier
//     colorClassifier.addBreaks(breaks).addColors(colors)
//     colorClassifier
//   }
// }


// /** An abstract class for automatically (through color interpolation) classifying a raster's values
//   *  in terms of color for rendering
//   *
//   * @tparam T  The underlying datatype being classified (implementations for Int and Double)
//   *
//   */
// abstract class BlendingColorClassifier[@specialized (Int, Double) T <: AnyVal] extends ColorClassifier[T] {
//   protected var classificationBreaks: mutable.Buffer[T] = mutable.Buffer[T]()
//   protected var classificationColors: mutable.Buffer[RGBA] = mutable.ArrayBuffer[RGBA]()
//   implicit val ctag: ClassTag[T]
//   implicit val ord: Ordering[T]

//   def length: Int = classificationBreaks.size

//   def getBreaks: Array[T] = classificationBreaks.toArray

//   def getColors: Array[RGBA] = classificationColors.toArray

//   def mapBreaks(f: T => T): ColorClassifier[T] = {
//     classificationBreaks = classificationBreaks map(f(_))
//     this
//   }

//   def mapColors(f: RGBA => RGBA): ColorClassifier[T] = {
//     classificationColors = classificationColors map(f(_))
//     this
//   }

//   def mapColorsToIndex(): ColorClassifier[T] = {
//     classificationColors = mutable.ListBuffer((0 to classificationColors.size).map(RGBA(_)):_*)
//     this
//   }

//   def addBreaks(breaks: Array[T]): ColorClassifier[T] =
//     addBreaks(breaks: _*)

//   def addBreaks(breaks: T*): ColorClassifier[T] = {
//     classificationBreaks ++= breaks
//     this
//   }

//   def addColors(colors: Array[RGBA]): ColorClassifier[T] =
//     addColors(colors: _*)

//   def addColors(colors: RGBA*): ColorClassifier[T] = {
//     classificationColors ++= colors
//     this
//   }

//   /** If the count of colors doesn't match the count of classification Breaks, produce a
//     * ColorClassifier which either interpolates or properly subsets the colors so as
//     * to have an equal count of Breaks and colors.
//     *
//     * @note normalization is not possible without at least one break and at least one color
//    **/
//   def normalize: ColorClassifier[T] = {
//     if (classificationBreaks.size == 0 || classificationColors.size == 0) {
//     } else if (classificationBreaks.size < classificationColors.size) {
//       classificationColors = spread(getColors, classificationBreaks.size).toBuffer
//     } else if (classificationBreaks.size > classificationColors.size) {
//       classificationColors = chooseColors(getColors, classificationBreaks.size).toBuffer
//     }
//     this
//   }

//   /** Transform this instances colors such that the transformed colors sit along an alpha gradient.
//     *
//     * @param start An integer whose last two bytes are the alpha value at the start of the
//     *              gradient (default 0)
//     * @param stop  An integer whose last two bytes are the alpha value at the end of the
//     *              gradient (default 255)
//     */
//   def alphaGradient(start: RGBA = RGBA(0), stop: RGBA = RGBA(0xFF)): ColorClassifier[T] = {
//     val colors = getColors
//     val alphas = chooseColors(start, stop, colors.length).map(_.alpha)

//     val newColors = colors.zip(alphas).map ({ case (color, a) =>
//       val (r, g, b) = color.unzipRGB
//       RGBA(r, g, b, a)
//     })

//     classificationColors = newColors.toBuffer
//     this
//   }

//   /** Set a uniform alpha value for all contained colors */
//   def setAlpha(a: Int): ColorClassifier[T] = {
//     val newColors = getColors.map { color =>
//       val(r, g, b) = color.unzipRGB
//       RGBA(r, g, b, a)
//     }
//     classificationColors = newColors.toBuffer
//     this
//   }

//   /** Set a uniform alpha value for all contained colors */
//   def setAlpha(alphaPct: Double): ColorClassifier[T] = {
//     val newColors: Array[RGBA] = getColors.map { color =>
//       val(r, g, b) = color.unzipRGB
//       RGBA(r, g, b, alphaPct)
//     }
//     classificationColors = newColors.toBuffer
//     this
//   }

//   /**
//     * This method will return a smaller list of colors the provided list of colors, spaced
//     * out amongst the provided color list. Used internally for normalization
//     *
//     * For example, if we are provided a list of 9 colors on a red
//     * to green gradient, but only need a list of 3, we expect to get back a
//     * list of 3 colors with the first being red, the second color being the 5th
//     * color (between red and green), and the last being green.
//     *
//     * @param colors  Provided RGBA color values
//     * @param n       Length of list to return
//     */
//   protected def spread(colors: Array[RGBA], n: Int): Array[RGBA] = {
//     if (colors.length == n) return colors

//     val colors2 = new Array[RGBA](n)
//     colors2(0) = colors(0)

//     val b = n - 1
//     val color = colors.length - 1
//     var i = 1
//     while (i < n) {
//       colors2(i) = colors(math.round(i.toDouble * color / b).toInt)
//       i += 1
//     }
//     colors2
//   }

//   /** RGBA interpolation logic */



//    def blend(start: Int, end: Int, numerator: Int, denominator: Int): Int = {
//     start + (((end - start) * numerator) / denominator)
//   }

//    def chooseColors2(c: Array[RGBA], numColors: Int): Array[RGBA] =
//     getColorSequence(numColors) { (masker: RGBA => Int, count: Int) =>
//       val hues = c.map(masker)
//       val mult = c.length - 1
//       val denom = count - 1

//       if (count < 2) {
//         Array(hues(0))
//       } else {
//         val ranges = new Array[Int](count)
//         var i = 0
//         while (i < count) {
//           val j = (i * mult) / denom
//           ranges(i) = if (j < mult) {
//             blend(hues(j), hues(j + 1), (i * mult) % denom, denom)
//           } else {
//             hues(j)
//           }
//           i += 1
//         }
//         ranges
//       }
//     }

//    def chooseColors(color1: RGBA, color2: RGBA, numColors: Int): Array[RGBA] =
//     getColorSequence(numColors) { (masker: RGBA => Int, count: Int) =>
//       val start = masker(color1)
//       val end   = masker(color2)
//       if (numColors < 2) {
//         Array(start)
//       } else {
//         val ranges = new Array[Int](numColors)
//         var i = 0
//         while (i < numColors) {
//           ranges(i) = blend(start, end, i, numColors - 1)
//           i += 1
//         }
//         ranges
//       }
//     }

//   /** Returns a sequence of RGBA integer values */
//    def getColorSequence(n: Int)(getRanges: (RGBA => Int, Int) => Array[Int]): Array[RGBA] = {
//     val unzipR = { color: RGBA => color.red }
//     val unzipG = { color: RGBA => color.green }
//     val unzipB = { color: RGBA => color.blue }
//     val unzipA = { color: RGBA => color.alpha }
//     val rs = getRanges(unzipR, n)
//     val gs = getRanges(unzipG, n)
//     val bs = getRanges(unzipB, n)
//     val as = getRanges(unzipA, n)

//     val theColors = new Array[RGBA](n)
//     var i = 0
//     while (i < n) {
//       theColors(i) = RGBA(rs(i), gs(i), bs(i), as(i))
//       i += 1
//     }
//     theColors
//   }
// }



// /** A concrete class for automatic (interpolating to generate breaks' corresponding colors)
//   * color classification of int-based tiles
//   *
//   * @param classificationType The ClassBoundaryType which specifies the sort of boundary used in
//   *                           color classification
//   */
// case class BlendingIntColorClassifier(classificationType: ClassBoundaryType = LessThan)
//     extends BlendingColorClassifier[Int] {
//   def toColorMap(): ColorMap =
//     ColorMap(getBreaks, getColors.map(_.int), cmapOptions)

//   /** Converts this color classifier based on a histogram. This is an optimization */
//   def toColorMap(histogram: Histogram[Int]): ColorMap = {
//     val colorMap: IntColorMap = ColorMap(getBreaks, getColors.map(_.int), cmapOptions)
//     colorMap.cache(histogram)
//   }
// }

// /** A concrete class for automatic (interpolating to generate breaks' corresponding colors)
//   * color classification of double-based tiles
//   *
//   * @param classificationType The ClassBoundaryType which specifies the sort of boundary used in
//   *                           color classification
//   */
// case class BlendingDoubleColorClassifier(classificationType: ClassBoundaryType = LessThan)
//     extends BlendingColorClassifier[Double] {
//   def toColorMap(): ColorMap =
//     ColorMap(getBreaks, getColors.map(_.int), cmapOptions)
// }
