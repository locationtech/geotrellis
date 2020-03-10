/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.mapalgebra.focal

import geotrellis.vector.{Extent, Point}

import org.apache.commons.math3.analysis.interpolation._

import squants.space.{LengthUnit, Feet, Meters}


/** Produces a ZFactor for a given point using the prescribed
 *  conversion method.
 *
 *  @param produceZFactor A function that takes a latitude point and
 *    converts it to its corresponding ZFactor.
 */
class ZFactor(produceZFactor: Double => Double) extends Serializable {
  /** Produces the associated ZFactor for the given location.
   *
   *  @param extent The center point of this [[Extent]] will be used to
     *  determine the ZFactor.
   */
  def fromExtent(extent: Extent): Double =
    fromPoint(extent.center)

  /** Produces the associated ZFactor for the given `Point`'s Y coordinate. */
  def fromPoint(point: Point): Double =
    fromLatitude(point.getY)

  def fromLatitude(lat: Double): Double =
    produceZFactor(lat)
}

/** When creating a ZFactor instance, the projection of the target
 *  Tile(s) needs to be taken into account. If the Tiles are in
 *  LatLng, then the conversion between Latitude and ZFactor can
 *  already be calculated. Otherwise, one will need to supply the
 *  transformation required to produce the ZFactor.
 */
object ZFactor {
  final val LAT_LNG_FEET_AT_EQUATOR = 365217.6
  final val LAT_LNG_METERS_AT_EQUATOR = 111320

  /** Creates a [[ZFactor]] instance. specifically for Tiles
   *  that are in LatLng.
   *
   *  @param unit The unit of measure that the Tile(s) are in. Only two
   *    `LengthUnit`s are supported, `Feet` and `Meters`.
   */
  def forLatLng(unit: LengthUnit): ZFactor =
    unit match {
      case Feet =>
        ZFactor((lat: Double) => 1 / (LAT_LNG_FEET_AT_EQUATOR * math.cos(math.toRadians(lat))))
      case Meters =>
        ZFactor((lat: Double) => 1 / (LAT_LNG_METERS_AT_EQUATOR * math.cos(math.toRadians(lat))))
      case _ =>
        throw new Exception(s"Could not create a ZFactor instance from the following unit: ${unit}. Please use either Feet or Meters")
    }

  /** Creates a [[ZFactor]] instance which uses linear interplation to calculate
   *  ZFactors. The linear interploation itself is derived from the values given.
   *
   *  @param mappedLatitudes A Map that maps latitudes to ZFactors. It is not required
   *    to supply a ZFactor for every latitude intersected by the Tile(s). Rather,
   *    based on the values given, a linear interpolation will be created and
   *    any latitude not mapped will have its associated ZFactor derived from
   *    that interpolation.
   */
  def interpolateFromTable(mappedLatitudes: Map[Double, Double]): ZFactor = {
    val interp = new LinearInterpolator()
    val spline = interp.interpolate(mappedLatitudes.keys.toArray, mappedLatitudes.values.toArray)

    ZFactor((lat: Double) => spline.value(lat))
  }

  def apply(produceZFactor: Double => Double): ZFactor =
    new ZFactor(produceZFactor)
}
