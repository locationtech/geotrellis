/***
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
 ***/

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.TileNeighbors

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
   * @param      r      Raster to for which to compute the hill shading.
   * @param      tns    TileNeighbors that describe the neighboring tiles.
   */
  def apply(r:Op[Raster],tns:Op[TileNeighbors]):DirectHillshade = DirectHillshade(r,tns,315.0,45.0,1.0)


  def apply(r:Op[Raster]):DirectHillshade = DirectHillshade(r,315.0,45.0,1.0)

  /**
   * Creates a hillshade operation using a precomputed slope and aspect.
   *
   * @param   aspect      [[Aspect]] operation
   * @param   slope       [[Slope]] operation
   * @param   azimuth     Degrees clockwise from north of light source.
   * @param   altitude    Degrees above horizon of the light source.
   */
  def apply(aspect:Aspect,slope:Slope,azimuth:Op[Double],altitude:Op[Double]) =
    IndirectHillshade(aspect,slope,azimuth,altitude)

  /**
   * Creates a hillshade operation directly from azimuth, altitude, and zFactor parameters.
   *
   * @param   r           Raster to for which to compute the hill shading.
   * @param   tns         TileNeighbors that describe the neighboring 
   * @param   azimuth     Degrees clockwise from north of light source.
   * @param   altitude    Degrees above horizon of the light source.
   * @param   zFactor     Factor that convers altitude units to map units.
   *                      (map units per elevation unit)
   */
  def apply(r:Op[Raster],tns:Op[TileNeighbors],azimuth:Op[Double],altitude:Op[Double],zFactor:Op[Double]) =
    DirectHillshade(r,tns,azimuth,altitude,zFactor)

  def apply(r:Op[Raster],azimuth:Op[Double],altitude:Op[Double],zFactor:Op[Double]) =
    DirectHillshade(r,azimuth,altitude,zFactor)
}

/**
 * Direct calculation of hill shading of a raster. Construct through the [[Hillshade]] object.
 *
 * @see [[Hillshade]]
 */
case class DirectHillshade(r:Op[Raster],tns:Op[TileNeighbors],azimuth:Op[Double],altitude:Op[Double],zFactor:Op[Double])
    extends FocalOp3[Double,Double,Double,Raster](r,Square(1),tns,azimuth,altitude,zFactor)({
  (raster,n) => new SurfacePointCalculation[Raster] with ShortRasterDataResult 
                                                    with Initialization3[Double,Double,Double] {
    var azimuth = 0.0
    var zenith = 0.0
    var zFactor = 0.0

    // Caches trig values for speed
    var cosZ = 0.0
    var sinZ = 0.0
    var cosAz = 0.0
    var sinAz = 0.0

    def init(r:Raster,az:Double,al:Double,z:Double) = {
      super.init(r)

      azimuth = radians(90.0 - az)
      zenith = radians(90.0 - al)
      zFactor = z

      cosZ = cos(zenith)
      sinZ = sin(zenith)
      cosAz = cos(azimuth)
      sinAz = sin(azimuth)
    }

    def setValue(x:Int,y:Int,s:SurfacePoint) {
      val slope = s.slope(zFactor)
      val aspect = s.aspect

      val c = cosAz*s.cosAspect + sinAz*s.sinAspect // cos(azimuth - aspect)
      val v = (cosZ * s.cosSlope) + (sinZ * s.sinSlope * c)
      data.set(x,y,round(127.0 * max(0.0,v)).toInt)      
    }
  }
}) 

object DirectHillshade {
  def apply(r:Op[Raster],azimuth:Op[Double],altitude:Op[Double],zFactor:Op[Double]) = new DirectHillshade(r,TileNeighbors.NONE,azimuth,altitude,zFactor)
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
case class IndirectHillshade(aspect:Aspect,slope:Slope,azimuth:Op[Double],altitude:Op[Double]) 
     extends Operation[Raster] {
  def _run() = runAsync(List('init,aspect,slope,azimuth,altitude))
  def productArity = 4
  def canEqual(other:Any) = other.isInstanceOf[IndirectHillshade]
  def productElement(n:Int) = n match {
    case 0 => aspect
    case 1 => slope
    case 2 => azimuth
    case 3 => altitude
    case _ => new IndexOutOfBoundsException()
  }
  val nextSteps:PartialFunction[Any,StepOutput[Raster]] = {
    case 'init :: (aspect:Raster) :: (slope:Raster) :: (azimuth:Double) :: (altitude:Double) :: Nil => 
      val az = radians(90.0 - azimuth)
      val ze = radians(90.0 - altitude)
      val cosZe = cos(ze)
      val sinZe = sin(ze)
      
      val hr = aspect.combineDouble(slope) { (aspectValue,slopeValue) =>
        val slopeRads = radians(slopeValue)
        val aspectRads = radians(aspectValue)
        val v = (cosZe * cos(slopeRads)) +
         (sinZe * sin(slopeRads) * cos(az - aspectRads))
        round(127.0 * max(0.0,v))
      }
      Result(hr.convert(TypeShort))
  }
}

