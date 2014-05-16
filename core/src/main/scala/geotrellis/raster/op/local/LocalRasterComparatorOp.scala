package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.BitArrayRasterData
import scalaxy.loops._

trait LocalRasterComparatorOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0,n.length-1)
    else n
  }
  // Raster - Constant combinations

  /** Apply to the value from each cell and a constant Int. */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = {
    (r,c).map(apply)
      .withName(s"$name[ConstantInt]")
  }

  /** Apply to the value from each cell and a constant Double. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] =
    (r,c).map(apply)
      .withName(s"$name[ConstantDouble]")

  /** Apply to a constant Int and the value from each cell. */
  def apply(c:Op[Int],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI):Op[Raster] =
    (c,r).map(apply)
      .withName(s"$name[ConstantInt]")

  /** Apply to a constant Double and the value from each cell. */
  def apply(c:Op[Double],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI):Op[Raster] =
    (c,r).map(apply)
      .withName(s"$name[ConstantDouble]")

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Raster, c: Int): Raster = {
    val data = BitArrayRasterData.ofDim(r.cols, r.rows)
    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        data.set(col,row, if(compare(r.get(col, row), c)) 1 else 0)
      }
    }
    Raster(data, r.rasterExtent)
  }

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Raster, c: Double): Raster = {
    val data = BitArrayRasterData.ofDim(r.cols, r.rows)

    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        data.set(col,row, if(compare(r.getDouble(col, row), c)) 1 else 0)
      }
    }
    Raster(data, r.rasterExtent)
  }

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Int, r: Raster): Raster = {
    val data = BitArrayRasterData.ofDim(r.cols, r.rows)

    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        data.set(col,row, if(compare(c, r.get(col, row))) 1 else 0)
      }
    }
    Raster(data, r.rasterExtent)
  }

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Double, r: Raster): Raster = {
    val data = BitArrayRasterData.ofDim(r.cols, r.rows)

    for(col <- 0 until r.cols optimized) {
      for(row <- 0 until r.rows optimized) {
        data.set(col,row, if(compare(c, r.getDouble(col, row))) 1 else 0)
      }
    }
    Raster(data, r.rasterExtent)
  }

  // Raster - Raster combinations

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI):Op[Raster] =
    (r1,r2).map(apply)
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Raster,r2: Raster): Raster = {
    val data = BitArrayRasterData.ofDim(r1.cols, r1.rows)

    for(col <- 0 until r1.cols optimized) {
      for(row <- 0 until r1.rows optimized) {
        if(r1.rasterType.isDouble) {
          data.set(col,row, if(compare(r1.getDouble(col, row), r2.getDouble(col, row))) 1 else 0)
        }else {
          data.set(col,row, if(compare(r1.get(col, row), r2.get(col, row))) 1 else 0)
        }
      }
    }
    Raster(data, r1.rasterExtent)
  }


  def compare(z1: Int, z2: Int): Boolean
  def compare(z1: Double, z2: Double): Boolean
}