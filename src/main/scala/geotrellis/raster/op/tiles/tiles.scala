package geotrellis.raster.op.tiles

import scala.math.{ min, max }

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.raster._

abstract class Reducer1[B: Manifest, C: Manifest](r: Op[Raster])(handle: Raster => B)(reducer: List[B] => C) extends Op[C] {
  def _run(context: Context) = runAsync('init :: r :: Nil)

  val nextSteps: Steps = {
    case 'init :: (r: Raster) :: Nil => init(r)
    case 'reduce :: (bs: List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
  }

  def init(r: Raster) = {
    r.data match {
      case _: TiledRasterData => runAsync('reduce :: r.getTileList.map(mapper))
      case _ => Result(reducer(handle(r) :: Nil))
    }
  }

  def mapper(r: Raster): Op[B] = logic.Do1(r)(handle)
}

case class ThoroughputLimitedReducer1[B: Manifest, C: Manifest](r: Op[Raster], limit: Int = 30)(handle: Raster => B)(reducer: List[B] => C) extends Op[C] {
  def _run(context: Context) = {
    runAsync('init :: r :: Nil)
  }
  val nextSteps: Steps = {
    case 'init :: (r: Raster) :: Nil => init(r)
    case 'reduce :: (bs: List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
    case 'runGroup :: (oldResults: List[_]) :: (bs: List[_]) :: (newResults: List[_]) => {
      val results = oldResults ::: newResults
      bs match {
        case Nil => Result(reducer(results.asInstanceOf[List[B]]))
        case (head: List[_]) :: tail => {
          println("run next batch")
          runAsync('runGroup :: results :: tail :: head)
        }
      }
    }
  }

  def init(r: Raster) = {
    r.data match {
      case _: TiledRasterData => {
        val ops = r.getTileOpList.map(mapper)
        val groups = ops grouped (limit) toList
        val tail = groups.tail
        val head = groups.head
        runAsync('runGroup :: List[B]() :: tail :: head)
      }
      case _ => {
        Result(reducer(handle(r) :: Nil))
      }
    }
  }

  def mapper(r: Op[Raster]): Op[B] = logic.Do(r)(handle)
}

case class TileMin(r: Op[Raster]) extends Reducer1(r)({
  r =>
    {
      var zmin = Int.MaxValue
      r.foreach {
        z => if (z != NODATA) zmin = min(z, zmin)
      }
      zmin
    }
})({
  zs => zs.reduceLeft((x, y) => min(x, y))
})

case class TileMax(r: Op[Raster]) extends Reducer1(r)({
  r => r.findMinMax._2
})({
  zs => zs.reduceLeft((x, y) => max(x, y))
})

case class TileHistogram(r: Op[Raster]) extends Reducer1(r)({
  r => FastMapHistogram.fromRaster(r)
})({
  hs => FastMapHistogram.fromHistograms(hs)
})
