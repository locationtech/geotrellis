package geotrellis.raster.op.local

import geotrellis._

trait LocalRasterBinaryOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0,n.length-1)
    else n
  }

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI):Op[Raster] = 
    (r1,r2).map(doRasters)
           .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Seq[Op[Raster]]):Op[Raster] = 
    rs.mapOps(_.reduce(doRasters))
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Array[Op[Raster]]):Op[Raster] = 
    rs.mapOps(_.reduce(doRasters))
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Raster]]):Op[Raster] = 
    rs.map(_.reduce(doRasters))
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Array[Raster]])(implicit d:DI):Op[Raster] = 
    rs.map(_.reduce(doRasters))
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Op[Raster]]])(implicit d:DI,d2:DI):Op[Raster] =
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Raster]*)(implicit d:DI,d2:DI,d3:DI):Op[Raster] = {
    apply(rs)
  }

  def doRasters(r1:Raster,r2:Raster):Raster
}
