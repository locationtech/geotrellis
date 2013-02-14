package geotrellis.raster.op.focal

import scala.math.{ min, max }

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.raster._
import geotrellis.feature.Polygon

object TileFocalOp {
  /**
   * Used to make a [[TileFocalOp]] based off a focal operation that will
   * parallelize correctly with tiled raster data.
   */
  def makeFocalOp[T <: Op[Raster] with FocalOperationBase with HasAnalysisArea[T]](makeOp:Op[Raster] => T) = 
  (_r:Op[Raster], _re:Op[Option[RasterExtent]]) => {
    val op = makeOp(_r)
    op.setAnalysisArea(_re)
  }
}

/**
 * Used to make an operation based off a focal operation that will
 * parallelize correctly with tiled raster data.
 *
 * Use [[TileFocalOp.makeFocalOp]] to create correctly create a TileFocalOp.
 * 
 * @example
 * {{{
 * val tileFocalOp = TileFocalOp(tiledRaster, Min(_, Square(1)))
 * }}}
 */
case class TileFocalOp[T <:Op[Raster] with FocalOperationBase with HasAnalysisArea[T]](r: Op[Raster], zonalOp:(Op[Raster]) => T) extends Op[Raster] {
  def _run(context: Context) = runAsync('init :: r :: Nil)

  val nextSteps: Steps = {
    case 'init :: (r: Raster) :: Nil => init(r)
    case 'untiled :: (r:Raster) :: Nil => Result(r)
    case 'results :: (tileLayout:TileLayout) :: (re:RasterExtent) :: (r:List[_]) => { 
        val tiles = r.asInstanceOf[List[Raster]] 
        val firstTile = tiles.head
        val data = TileArrayRasterData(tiles.toArray, tileLayout, re)
        Result(Raster(data, re))
      }
  }

  def init(r: Raster) = {
    r.data match {
      case trd: TiledRasterData => tileFocalOp(r, trd, TileFocalOp.makeFocalOp(zonalOp))
      case _ => throw new Exception("TileFocalOp not implemented for non-tiled rasters")
    }
  }
  
  def tileFocalOp[T](raster:Raster, trd:TiledRasterData, makeFocalOp: (Op[Raster], Op[Option[RasterExtent]]) => Op[Raster]) = {
    val tileLayout = trd.tileLayout
    val re = raster.rasterExtent
    val rl = tileLayout.getResolutionLayout(re)
    val tileTuples = for (r <- 0 until tileLayout.tileRows; c <- 0 until tileLayout.tileCols)
      yield (c,r,rl.getRasterExtent(c,r))

    val ops = for ( (c:Int,r:Int,tileRe:RasterExtent) <- tileTuples) yield {
      val nRasterData = TileNeighborhood.buildTileNeighborhood(trd, re, c, r)
      val neighborhoodRaster = Raster(nRasterData, nRasterData.rasterExtent)
      val zOp = makeFocalOp(neighborhoodRaster, Some(tileRe)) 
      zOp
    }
    runAsync('results :: tileLayout :: re ::  ops.toList)
  }
}

