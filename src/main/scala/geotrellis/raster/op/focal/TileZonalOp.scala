package geotrellis.raster.op.focal

import scala.math.{ min, max }

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.raster._
import geotrellis.feature.Polygon

object TileZonalOp {
  def makeZonalOp[T <: Op[Raster] with FocalOperationBase with HasAnalysisArea[T]](makeOp:Op[Raster] => T) = 
  (_r:Op[Raster], _re:Op[Option[RasterExtent]]) => {
    val op = makeOp(_r)
    op.setAnalysisArea(_re)
  }
}
case class TileZonalOp[T <:Op[Raster] with FocalOperationBase with HasAnalysisArea[T]](r: Op[Raster], zonalOp:(Op[Raster]) => T) extends Op[Raster] {
  def _run(context: Context) = runAsync('init :: r :: Nil)

  val nextSteps: Steps = {
    case 'init :: (r: Raster) :: Nil => init(r)
    case 'untiled :: (r:Raster) :: Nil => Result(r)
    //case 'reduce :: (bs:  List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
    case 'results :: (tileLayout:TileLayout) :: (re:RasterExtent) :: (r:List[_]) => { 
        val tiles = r.asInstanceOf[List[Raster]] 
        val firstTile = tiles.head
        val data = TileArrayRasterData(tiles.toArray, tileLayout, re)
        Result(Raster(data, re))
      }
  }

  def init(r: Raster) = {
    r.data match {
      case trd: TiledRasterData => tileZonalOp(r, trd, TileZonalOp.makeZonalOp(zonalOp))
      //case _ => runAsync('untiled :: makeZonalOp(r, Some(r.rasterExtent)) :: Nil) 
    }
  }
  
  def tileZonalOp[T](raster:Raster, trd:TiledRasterData, makeZonalOp: (Op[Raster], Op[Option[RasterExtent]]) => Op[Raster]) = {
    val tileLayout = trd.tileLayout
    val re = raster.rasterExtent
    val rl = tileLayout.getResolutionLayout(re)
    val tileTuples = for (r <- 0 until tileLayout.tileRows; c <- 0 until tileLayout.tileCols)
      yield (c,r,rl.getRasterExtent(c,r))

    val ops = for ( (c:Int,r:Int,tileRe:RasterExtent) <- tileTuples) yield {
      val nRasterData = TileNeighborhood.buildTileNeighborhood(trd, re, c, r)
      val neighborhoodRaster = Raster(nRasterData, nRasterData.rasterExtent)
      val zOp = makeZonalOp(neighborhoodRaster, Some(tileRe)) 
      zOp
    }
    runAsync('results :: tileLayout :: re ::  ops.toList)
    //runAsync('results :: tileLayout :: re ::  List(ops.last))
  }
}

