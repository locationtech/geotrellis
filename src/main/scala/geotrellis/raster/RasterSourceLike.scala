package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._

trait RasterSourceLike[+Repr <: DataSource[Raster]] extends DataSourceLike[Raster, Repr] with DataSource[Raster] { self: Repr =>
  //type BF:CBSF[Repr, Raster, That]
  type CBF[A] = ({ type BF = CBSF[Repr, Raster, A]})#BF
  def tiles = self.partitions
  def rasterDefinition:Op[RasterDefinition]
  
  
  
  override def map[B,That](f:Op[Raster] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  {
    val builder = bf.apply(this)
	  val newOp = this.partitions.map( seq =>
	      seq.map {
	        op => f(op)
	      }
	)
	builder.setOp(newOp)
    builder.result()
  }
  
  def localAdd[That](i: Int)(implicit cbf: CBF[That]) = this.map(local.Add(_, i))

}
