// package geotrellis.raster.render

// import geotrellis.raster.histogram.Histogram

// import scala.collection.mutable
// import scala.reflect.ClassTag

// object StrictColorClassifier {
//   def apply(classifications: Array[(Int, RGBA)]): StrictIntColorClassifier =
//     apply(classifications, None)

//   def apply(classifications: Array[(Int, RGBA)], noDataColor: Option[RGBA]): StrictIntColorClassifier = {
//     val colorClassifier = new StrictIntColorClassifier
//     classifications foreach { case classification: (Int, RGBA) =>
//       colorClassifier.classify(classification._1, classification._2)
//     }
//     colorClassifier
//   }

//   def fromQuantileBreaks(histogram: Histogram[Int], colors: Array[RGBA]) = {
//     val breaks = histogram.quantileBreaks(colors.length)
//     apply(breaks zip colors)
//   }

//   def fromQuantileBreaksDouble(histogram: Histogram[Double], colors: Array[RGBA]) = {
//     val breaks = histogram.quantileBreaks(colors.length)
//     apply(breaks zip colors)
//   }

//   def apply(classifications: Array[(Double, RGBA)]): StrictDoubleColorClassifier =
//     apply(classifications, None)

//   def apply(classifications: Array[(Double, RGBA)], noDataColor: Option[RGBA]): StrictDoubleColorClassifier = {
//     val colorClassifier = new StrictDoubleColorClassifier
//     classifications foreach { case classification: (Double, RGBA) =>
//       colorClassifier.classify(classification._1, classification._2)
//     }
//     colorClassifier
//   }
// }


// /** An abstract class for strictly (explicitly) classifying a raster's values in terms of color
//   * for rendering
//   *
//   * @tparam T  The underlying datatype being classified (implementations for Int and Double)
//   *
//   */
// abstract class StrictColorClassifier[@specialized (Int, Double) T <: AnyVal](
//   val colorClassifications: Map[T, RGBA],
//   val fallbackColor: RGBA = ColorClassifier.DEFAULT_FALLBACK_COLOR
// ) extends ColorClassifier[T] {
//   // protected var colorClassifications: mutable.Map[T, RGBA] = mutable.Map[T, RGBA]()
//   // implicit val ctag: ClassTag[T]
//   // implicit val ord: Ordering[T]

//   def length = colorClassifications.size

//   def getBreaks: Array[T] = {
//     colorClassifications.keys.toArray.sorted
//   }

//   def getColors: Array[RGBA] =
//     getBreaks.map { break =>
//       colorClassifications(break)
//     }

//   def mapBreaks(f: T => T): ColorClassifier[T] = {
//     val newMap = mutable.Map[T, RGBA]()
//     colorClassifications map { case (k, v) =>
//       newMap(f(k)) = v
//     }
//     colorClassifications = newMap
//     this
//   }

//   def mapColors(f: RGBA => RGBA): ColorClassifier[T] = {
//     colorClassifications map { case (k, v) =>
//       colorClassifications(k) = v
//     }
//     this
//   }

//   /** Add a classification (T with a corresponding RGBA) to this classifier instance
//    *
//     * @param classBreak  A provided class boundary/breakpoint
//     * @param classColor  A provided RGBA color value
//    */
//   // def classify(classBreak: T, classColor: RGBA): ColorClassifier[T] = {
//   //   colorClassifications(classBreak) = classColor
//   //   this
//   // }

//   // /** Add color classifications to this classifier instance */
//   // def addClassifications(classifications: Array[(T, RGBA)]): ColorClassifier[T] =
//   //   addClassifications(classifications: _*)

//   // /** Add color classifications to this classifier instance */
//   // def addClassifications(classifications: (T, RGBA)*): ColorClassifier[T] = {
//   //   classifications map { case classification: (T, RGBA) =>
//   //     classify(classification._1, classification._2)
//   //   }
//   //   this
//   // }
// }

// /** A concrete class for strict (fully explicit) color classification of int-based tiles
//   */
// case class StrictIntColorClassifier(val colorClassifications: Map[Int, RGBA])
//     extends StrictColorClassifier[Int] {

//   def toColorMap(): ColorMap =
//     ColorMap(getBreaks, getColors.map(_.int), cmapOptions)
// }

// /** A concrete class for strict (fully explicit) color classification of double-based tiles
//   */
// case class StrictDoubleColorClassifier(val colorClassifications: Map[Int, RGBA])
//     extends StrictColorClassifier[Double] {

//   def toColorMap(): ColorMap =
//     ColorMap(getBreaks, getColors.map(_.int), cmapOptions)
// }
