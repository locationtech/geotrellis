package geotrellis.engine.op.local

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.op.local._

trait LocalRasterSourceSeqMethods extends RasterSourceSeqMethods {
  val rasterDefinition = rasterSources.head.rasterDefinition

  def applyOp(f: Seq[Op[Tile]]=>Op[Tile]) =
    RasterSource(
      rasterDefinition, 
      rasterSources
        .toSeq
        .map(_.tiles)
        .mapOps(_.transpose.map(f))
    )

  // /** Adds all the rasters in the sequence */
  def localAdd(): RasterSource = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Add(tiles) }
    }

  /** Takes the difference of the rasters in the sequence from left to right */
  def difference() = localSubtract

  /** Takes the difference of the rasters in the sequence from left to right */
  def localSubtract() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Subtract(tiles) }
    }

  /** Takes the product of the rasters in the sequence */
  def product() = localMultiply

  /** Takes the product of the rasters in the sequence */
  def localMultiply() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Multiply(tiles) }
    }

  /** Divides the rasters in the sequence from left to right */
  def localDivide() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Divide(tiles) }
    }

  /** Takes the max of each cell value */
  def max() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Max(tiles) }
    }

  /** Takes the min of each cell value */
  def min() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Min(tiles) }
    }

  /** Takes the logical And of each cell value */
  def and() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => And(tiles) }
    }

  /** Takes the logical Or of each cell value */
  def or() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Or(tiles) }
    }

  /** Takes the logical Xor of each cell value */
  def xor() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Xor(tiles) }
    }

  /** Raises each cell value to the power of the next raster, from left to right */
  def exponentiate() = 
    applyOp { tileOps =>
      logic.Collect(tileOps).map { tiles: Seq[Tile] => Pow(tiles) }
    }

}
