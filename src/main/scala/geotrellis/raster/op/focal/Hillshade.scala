package geotrellis.raster.op.focal

import geotrellis._

import scala.math._

import Angles._

object Hillshade {
  /**
   * Create a default hillshade raster, using a default azimuth and altitude.
   */
  def apply(r:Op[Raster]):DirectHillshade = DirectHillshade(r,315.0,45.0,1.0)

  def apply(aspect:Aspect,slope:Slope,azimuth:Op[Double],altitude:Op[Double]) =
    IndirectHillshade(aspect,slope,azimuth,altitude)

  def apply(r:Op[Raster],azimuth:Op[Double],altitude:Op[Double],zFactor:Op[Double]) =
    DirectHillshade(r,azimuth,altitude,zFactor)
}

case class DirectHillshade(r:Op[Raster], azimuth:Op[Double],altitude:Op[Double],zFactor:Op[Double])
    extends FocalOp3[Double,Double,Double,Raster](r,Square(1),azimuth,altitude,zFactor)({
  (raster,n) => new CellwiseCalculation[Raster] with ShortRasterDataResult 
                                                with Initialization3[Double,Double,Double] 
                                                with SurfacePointCalculation {
    var azimuth = 0.0
    var zenith = 0.0
    var zFactor = 0.0
    var cellWidth = 0.0
    var cellHeight = 0.0

    val s = new SurfacePoint

    var cosZ = 0.0
    var sinZ = 0.0
    var cosAz = 0.0
    var sinAz = 0.0

    var cols = 0
    var y = -1

    override val traversalStrategy = Some(TraversalStrategy.ScanLine)

    def init(r:Raster,az:Double,al:Double,z:Double,reOpt:Option[RasterExtent]) = {
      super.init(r,None)

      azimuth = radians(90.0 - az)
      zenith = radians(90.0 - al)
      zFactor = z
      cellWidth = r.rasterExtent.cellwidth
      cellHeight = r.rasterExtent.cellheight
      cols = r.cols

      cosZ = cos(zenith)
      sinZ = sin(zenith)
      cosAz = cos(azimuth)
      sinAz = sin(azimuth)
    }

    def reset() = { y += 1 ; resetCols(y) }
    def remove(r:Raster,x:Int,y:Int) = { }

    def setValue(x:Int,y:Int) {
      calcSurface(s,cellWidth,cellHeight)
      val slope = s.slope(zFactor)
      val aspect = s.aspect

      val c = cosAz*s.cosAspect + sinAz*s.sinAspect // cos(azimuth - aspect)
      val v = (cosZ * s.cosSlope) + (sinZ * s.sinSlope * c)
      data.set(x,y,round(127.0 * max(0.0,v)).toInt)      

      moveRight(x+1 == cols)
    }
  }
})

case class IndirectHillshade(aspect:Aspect,slope:Slope,azimuth:Op[Double],altitude:Op[Double]) 
         extends Operation[Raster] {
  def _run(context:Context) = runAsync(List('init,aspect,slope,azimuth,altitude))
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
        val v = (cosZe * cos(slopeValue)) +
         (sinZe * sin(slopeValue) * cos(az - aspectValue))
        round(127.0 * max(0.0,v))
      }
      Result(hr.convert(TypeShort))
  }
}
