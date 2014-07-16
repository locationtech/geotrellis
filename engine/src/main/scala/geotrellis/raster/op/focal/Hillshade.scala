/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.engine._

import scala.math._

import Angles._

/**
 * Computes Hillshade (shaded relief) from a raster.
 *
 * The resulting raster will be a shaded relief map (a hill shading)
 * based on the sun altitude, azimuth, and the z factor. The z factor is
 * a conversion factor from map units to elevation units. 
 *
 * Returns a raster of TypeShort.
 * 
 * This operation uses Horn's method for computing hill shading.
 *
 * @see [[http://goo.gl/DtVDQ Esri Desktop's description of Hillshade.]]
 */
object Hillshade {
  /**
   * Create a default hillshade raster, using a default azimuth of 315 degrees,
   * altitude of 45 degrees, and z factor of 1.0.
   *
   * @param      r         Tile to for which to compute the hill shading.
   * @param      cellSize  cellSize of the raster
   * @param      tns       TileNeighbors that describe the neighboring tiles.
   */
  def apply(r: Op[Tile], tns: Op[TileNeighbors], cellSize: Op[CellSize]): DirectHillshade = 
    DirectHillshade(r, tns, cellSize, 315.0, 45.0, 1.0)


  def apply(r: Op[Tile], cellSize: Op[CellSize]): DirectHillshade = 
    DirectHillshade(r, cellSize, 315.0, 45.0, 1.0)

  /**
   * Creates a hillshade operation using a precomputed slope and aspect.
   *
   * @param   aspect      [[Aspect]] operation
   * @param   slope       [[Slope]] operation
   * @param   azimuth     Degrees clockwise from north of light source.
   * @param   altitude    Degrees above horizon of the light source.
   */
  def apply(aspect: Aspect, slope: Slope, azimuth: Op[Double], altitude: Op[Double]) =
    IndirectHillshade(aspect, slope, azimuth, altitude)

  /**
   * Creates a hillshade operation directly from azimuth, altitude, and zFactor parameters.
   *
   * @param   r           Tile to for which to compute the hill shading.
   * @param   tns         TileNeighbors that describe the neighboring 
   * @param   cellSize    cellSize of the raster
   * @param   azimuth     Degrees clockwise from north of light source.
   * @param   altitude    Degrees above horizon of the light source.
   * @param   zFactor     Factor that convers altitude units to map units.
   *                      (map units per elevation unit)
   */
  def apply(r: Op[Tile], tns: Op[TileNeighbors], cellSize: Op[CellSize], azimuth: Op[Double], altitude: Op[Double], zFactor: Op[Double]) =
    DirectHillshade(r, tns, cellSize, azimuth, altitude, zFactor)

  def apply(r: Op[Tile], cellSize: Op[CellSize], azimuth: Op[Double], altitude: Op[Double], zFactor: Op[Double]) =
    DirectHillshade(r, cellSize, azimuth, altitude, zFactor)
}

/**
 * Direct calculation of hill shading of a raster. Construct through the [[Hillshade]] object.
 *
 * @see [[Hillshade]]
 */
case class DirectHillshade(r: Op[Tile], tns: Op[TileNeighbors], cellSize: Op[CellSize], azimuth: Op[Double], altitude: Op[Double], zFactor: Op[Double])
    extends FocalOperation4[CellSize, Double, Double, Double, Tile](r, Square(1), tns, cellSize, azimuth, altitude, zFactor)
{
  override def getCalculation(r: Tile, n: Neighborhood) = HillshadeCalculation(r, n)
}

object DirectHillshade {
  def apply(r: Op[Tile], cellSize: Op[CellSize], azimuth: Op[Double], altitude: Op[Double], zFactor: Op[Double]) = new DirectHillshade(r, TileNeighbors.NONE, cellSize, azimuth, altitude, zFactor)
}

/**
 * Indirect calculation of hill shading of a raster that uses Aspect and Slope operation results.
 *
 * Construct through the [[Hillshade]] object.
 *
 * @note               Does not currently work with TiledFocalOps of Aspect and Slope.
 *                     Use the direct method with TileFocalOps and tiled raster data.
 * @see [[Hillshade]]
 */
case class IndirectHillshade(aspect: Aspect, slope: Slope, azimuth: Op[Double], altitude: Op[Double]) 
     extends Operation[Tile] {
  def _run() = runAsync(List('init, aspect, slope, azimuth, altitude))
  def productArity = 4
  def canEqual(other: Any) = other.isInstanceOf[IndirectHillshade]
  def productElement(n: Int) = n match {
    case 0 => aspect
    case 1 => slope
    case 2 => azimuth
    case 3 => altitude
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps: PartialFunction[Any, StepOutput[Tile]] = {
    case 'init :: (aspect: Tile) :: (slope: Tile) :: (azimuth: Double) :: (altitude: Double) :: Nil =>
      Result(HillshadeCalculation.indirect(aspect, slope, azimuth, altitude))
  }
}

