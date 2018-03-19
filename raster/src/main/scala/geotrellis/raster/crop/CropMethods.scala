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

package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.util.MethodExtensions


/**
  * A trait housing extension methods for cropping.
  */
trait CropMethods[T] extends MethodExtensions[T] {
  import Crop.Options

  /**
    * Given a [[GridBounds]] and some cropping options, crop.
    */
  def crop(gb: GridBounds, options: Options): T

  /**
    * Given a [[GridBounds]], crop.
    */
  def crop(gb: GridBounds): T =
    crop(gb, Options.DEFAULT)

  /**
   * Crop out multiple [[GridBounds]] windows.
   */
  def crop(windows: Seq[GridBounds]): Iterator[(GridBounds, T)] = {
    windows.toIterator.map { gb => (gb, crop(gb))}
  }

  /**
    * Given a number of columns and rows for the desired output and
    * some cropping options, crop.
    */
  def crop(cols: Int, rows: Int, options: Options): T =
    crop(GridBounds(0, 0, cols - 1, rows - 1), options)

  /**
    * Given a number of columns and rows for the desired output, crop.
    */
  def crop(cols: Int, rows: Int): T =
    crop(cols, rows, Options.DEFAULT)

  /**
    * Given the starting and stopping columns and rows and some
    * cropping options, crop.
    */
  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int, options: Options): T =
    crop(GridBounds(colMin, rowMin, colMax, rowMax), options)

  /**
    * Given the starting and stopping columns and rows, crop.
    */
  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): T =
    crop(colMin, rowMin, colMax, rowMax, Options.DEFAULT)
}
