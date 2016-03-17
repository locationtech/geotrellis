// /*
//  * Copyright (c) 2014 Azavea.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  * http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package geotrellis.raster.render

// import geotrellis.raster.histogram.Histogram
// import java.util.Locale

// import scala.collection.mutable
// import scala.reflect.ClassTag
// import scala.util.Try

// object ColorClassifier {
//   case class Options(
//     noDataColor: RGBA =  RGBA(0x00000000),
//     fallbackColor: RGBA = RGBA(0x00000000),
//     classBoundryType: ClassBoundryType = LessThan,

//   )
// }

// /** An abstract class for classifying a raster's values in terms of color for rendering
//  *
//  * @tparam T     The underlying datatype being classified (implementations for Int and Double)
//  *
//  */
// abstract class ColorClassifier[@specialized (Int, Double) T <: AnyVal](options: ColorClassifier.Options) extends Serializable {
//   /** Return a sorted list of classification boundaries */
//   def breaks: Array[T]

//   /** Return a sorted list of classification colors */
//   def colors: Array[RGBA]

//   /** Transform a color classifier's boundaries according to a provided function */
//   def mapBreaks(f: T => T): ColorClassifier[T]

//   /** Transform a color classifier's colors according to a provided function */
//   def mapColors(f: RGBA => RGBA): ColorClassifier[T]

//   /** Transform each of the colors in the color classifier to their index in the sequence of colors.
//     * This is useful for things like indexed PNG encodings based on color maps.
//     */
//   def mapColorsToIndex(): ColorClassifier[T]

//   /** Return a colormap for this classifier instance */
//   def toColorMap(): ColorMap

//   /** Return the number of classifications */
//   def length: Int

//   /** Return options for the construction of a ColorMap */
//   def colorMapOptions: ColorMapOptions =
//     ColorMapOptions(classificationType, noDataColor.int, fallbackColor.int, false)

// }
