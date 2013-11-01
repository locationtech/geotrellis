package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._

trait LocalRasterBinaryOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0,n.length-1)
    else n
  }

  /** Apply to the value from each cell and a constant Int. */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map { (r,c) => 
            r.dualMap(combine(_,c))({
              val d = i2d(c)
              (z:Double) => combine(z,d)
            })
          }
         .withName("$name[ConstantInt]")

  /** Apply to the value from each cell and a constant Double. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map { (r,c) => 
            r.dualMap({z => d2i(combine(i2d(z),c))})(combine(_,c))
          }
         .withName("$name[ConstantDouble]")

  /** Apply to a constant Int and the value from each cell. */
  def apply(c:Op[Int],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    (r,c).map { (r,c) => 
            r.dualMap(combine(c,_))({
              val d = i2d(c)
              (z:Double) => combine(d,z)
            })
          }
         .withName("$name[ConstantInt]")

  /** Apply to a constant Double and the value from each cell. */
  def apply(c:Op[Double],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI):Op[Raster] = 
    (r,c).map { (r,c) => 
            r.dualMap({z => d2i(combine(c,i2d(z)))})(combine(c,_))
          }
         .withName("$name[ConstantDouble]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI):Op[Raster] = 
    (r1,r2).map(_.dualCombine(_)(combine)(combine))
           .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Seq[Op[Raster]]):Op[Raster] = 
    rs.mapOps { rasters =>
        rasters.head.dualReduce(rasters.tail)(combine)(combine)
       }
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Array[Op[Raster]]):Op[Raster] = 
    apply(rs.toSeq)

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Raster]]):Op[Raster] = 
    apply(rs.map(_.map(Literal(_))))

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Array[Raster]])(implicit d:DI):Op[Raster] = 
    apply(rs.map(_.toSeq))

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Op[Raster]]])(implicit d:DI,d2:DI):Op[Raster] =
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Raster]*)(implicit d:DI,d2:DI,d3:DI):Op[Raster] = {
    apply(rs)
  }

  def combine(z1:Int,z2:Int):Int
  def combine(z1:Double,z2:Double):Double
}
