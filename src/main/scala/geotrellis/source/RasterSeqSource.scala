package geotrellis.source

import geotrellis._
import geotrellis.raster.op.local._

object RasterSeqSource {
  implicit def seqToRasterSourceSeq(seq:Seq[RasterSource]):RasterSeqSource =
    apply(seq)

  def apply(seq:Seq[RasterSource]):RasterSeqSource = {
    val op = logic.Collect(seq.map(_.tiles)).withName("RasterSeqSource-Collect")
                  .map(_.transpose.map(tiles => logic.Collect(tiles).withName("Collect-Tiles")))
    RasterSeqSource(seq.head.rasterDefinition,op)
  }
}

/** Represents a sequence of RasterSources */
case class RasterSeqSource(rasterDefinition:Op[RasterDefinition], elements:Op[Seq[Op[Seq[Raster]]]]) 
    extends DataSource[Seq[Raster],Seq[Raster]]
       with DataSourceLike[Seq[Raster],Seq[Raster],RasterSeqSource] {
  def convergeOp: Op[Seq[Raster]] =
    (rasterDefinition,logic.Collect(elements)).map { (rd,tileSeq) =>
      tileSeq.transpose.map { rasterTiles =>
        TileRaster(rasterTiles,rd.re,rd.tileLayout).toArrayRaster
      }
  }

  /** Adds all the rasters in the sequence */
  def sum() = localAdd
  /** Adds all the rasters in the sequence */
  def localAdd() = map(Add(_)).mapOp(_.withName("Add[RasterSeq]"))
  /** Takes the difference of the rasters in the sequence from left to right */
  def difference() = localSubtract
  /** Takes the difference of the rasters in the sequence from left to right */
  def localSubtract() = map(Subtract(_)).mapOp(_.withName("Subtract[RasterSeq]"))
  /** Takes the product of the rasters in the sequence */
  def product() = localMultiply
  /** Takes the product of the rasters in the sequence */
  def localMultiply() = map(Multiply(_)).mapOp(_.withName("Multiply[RasterSeq]"))
  /** Divides the rasters in the sequence from left to right */
  def localDivide() = map(Multiply(_)).mapOp(_.withName("Multiply[RasterSeq]"))

  /** Takes the max of each cell value */
  def max() = map(Max(_))
  /** Takes the min of each cell value */
  def min() = map(Min(_))

  /** Takes the logical And of each cell value */
  def and() = map(And(_))
  def or() = map(Or(_))
  def xor() = map(Xor(_))

  /** Raises each cell value to the power of the next raster, from left to right */
  def exponentiate() = map(Pow(_))

  /** Sets cells to 1 if all cell values are equal, otherwise 0 */
  def areEqual() = map(Equal(_))
  /** Sets cells to 1 if all cell values are unequal, otherwise 0 */
  def areDistinct() = map(Unequal(_))
  /** Sets cells to 1 if the cell values strictly descend, else 0 */
  def isDecending() = map(Greater(_))
  /** Sets cells to 1 if the cell values descend or are equal, else 0 */
  def isDecendingOrEqual() = map(GreaterOrEqual(_))
  /** Sets cells to 1 if the cell values strictly descend, else 0 */
  def isAscending() = map(Less(_))
  /** Sets cells to 1 if the cell values descend or are equal, else 0 */
  def isAcendingOrEqual() = map(LessOrEqual(_))
}

case class RasterSeqSource2(seq:Seq[RasterSource]) {
  def localAdd():RasterSource = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(seq.head.rasterDefinition)
    builder.setOp(
      seq.map(_.tiles).mapOps(_.transpose.map(Add(_)))
    )
    builder.result
  }
}
