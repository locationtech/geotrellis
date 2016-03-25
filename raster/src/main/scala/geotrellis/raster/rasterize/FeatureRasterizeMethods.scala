/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.raster.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterize.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Geometry,Feature}


/**
  * A trait providing extension methods for invoking the rasterizer on
  * [[Feature]] objects.
  */
trait FeatureIntRasterizeMethods[+G <: Geometry] extends MethodExtensions[Feature[G, Int]] {

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present [[Feature]].  The precise
    * definition of the word "covered" is determined by the options
    * parameter.
    */
  def foreach(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn: (Int, Int) => Unit): Unit =
    self.geom.foreach(re, options)(fn)

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present [[Feature]].  The precise
    * definition of the word "covered" is determined by the options
    * parameter.  The result is a [[Raster]].
    */
  def rasterize(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn: (Int, Int) => Int): Raster[ArrayTile] =
    self.geom.rasterize(re, options)(fn)

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present [[Feature]].  The precise
    * definition of the word "covered" is determined by the options
    * parameter.  The result is a [[Raster]].
    */
  def rasterizeDouble(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn: (Int, Int) => Double): Raster[ArrayTile] =
    self.geom.rasterizeDouble(re, options)(fn)

  /**
    * Fill in 'value' at each cell of given [[RasterExtent]] that is
    * covered by the present [[Feature]].  The result is a [[Raster]].
    */
  def rasterize(re: RasterExtent, value: Int): Raster[ArrayTile] =
    self.geom.rasterize(re, value)

  /**
    * Fill in 'value' at each cell of given [[RasterExtent]] that is
    * covered by the present [[Feature]].  The result is a [[Raster]].
    */
  def rasterizeDouble(re: RasterExtent, value: Double): Tile =
    self.geom.rasterizeDouble(re, value)

  /**
    * Rasterize the present [[Feature]] into the given
    * [[RasterExtent]].  The result is a [[Raster]].
    */
  def rasterize(re: RasterExtent): Raster[ArrayTile] =
    self.geom.rasterize(re, self.data)

  /**
    * Rasterize the present [[Feature]] into the given
    * [[RasterExtent]].  The result is a [[Raster]].
    */
  def rasterizeDouble(re: RasterExtent): Raster[ArrayTile] =
    self.geom.rasterizeDouble(re, self.data.toDouble)
}

/**
  * A trait providing extension methods for invoking the rasterizer on
  * [[Feature]] objects.
  */
trait FeatureDoubleRasterizeMethods[+G <: Geometry] extends MethodExtensions[Feature[G, Double]] {

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present [[Feature]].  The precise
    * definition of the word "covered" is determined by the options
    * parameter.
    */
  def foreach(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn: (Int, Int) => Unit): Unit =
    self.geom.foreach(re, options)(fn)

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present [[Feature]].  The precise
    * definition of the word "covered" is determined by the options
    * parameter.  The result is a [[Raster]].
    */
  def rasterize(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn: (Int, Int) => Int): Raster[ArrayTile] =
    self.geom.rasterize(re, options)(fn)

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present [[Feature]].  The precise
    * definition of the word "covered" is determined by the options
    * parameter.  The result is a [[Raster]].
    */
  def rasterizeDouble(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn: (Int, Int) => Double): Raster[ArrayTile] =
    self.geom.rasterizeDouble(re, options)(fn)

  /**
    * Fill in 'value' at each cell of given [[RasterExtent]] that is
    * covered by the present [[Feature]].  The result is a [[Raster]].
    */
  def rasterize(re: RasterExtent, value: Int): Raster[ArrayTile] =
    self.geom.rasterize(re, value)

  /**
    * Fill in 'value' at each cell of given [[RasterExtent]] that is
    * covered by the present [[Feature]].  The result is a [[Raster]].
    */
  def rasterizeDouble(re: RasterExtent, value: Double): Raster[ArrayTile] =
    self.geom.rasterizeDouble(re, value)

  /**
    * Rasterize the present [[Feature]] into the given
    * [[RasterExtent]].  The result is a [[Raster]].
    */
  def rasterize(re: RasterExtent): Raster[ArrayTile] =
    self.geom.rasterize(re, self.data.toInt)

  /**
    * Rasterize the present [[Feature]] into the given
    * [[RasterExtent]].  The result is a [[Raster]].
    */
  def rasterizeDouble(re: RasterExtent): Raster[ArrayTile] =
    self.geom.rasterizeDouble(re, self.data)
}
