package geotrellis.raster.op.tiles

import scala.math.{ min, max }

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.raster._
import geotrellis.feature.Polygon

abstract class Reducer1[B: Manifest, C: Manifest](r: Op[Raster])(handle: Raster => B)(reducer: List[B] => C) extends Op[C] {
  def _run(context: Context) = runAsync('init :: r :: Nil)

  val nextSteps: Steps = {
    case 'init :: (r: Raster) :: Nil => init(r)
    case 'reduce :: (bs:  List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
  }

  def init(r: Raster) = {
    r.data match {
      case _: TiledRasterData => runAsync('reduce :: r.getTileList.map(mapper))
      case _ => Result(reducer(handle(r) :: Nil))
    }
  }

  def mapper(r: Raster): Op[B] = logic.Do1(r)(handle)
}

trait ThroughputLimitedReducer1[C] extends Op[C] {
  type B
  
  val r: Op[Raster]
  val limit: Int = 30

  def loadTileExtent:Option[Op[Polygon[_]]] = None

  def mapper(r: Op[Raster]): Op[List[B]]
  def reducer(mapResults: List[B]): C

  def _run(context: Context) = {
    loadTileExtent match {
      case None => runAsync('init :: r :: Nil)
      case Some(op) => runAsync('initWithTileExtent :: r :: op :: Nil)
    }
  }

  val nextSteps: Steps = {
    case 'init :: (r: Raster) :: Nil => init(r, None)
    case 'initWithTileExtent :: (r: Raster) :: (p:Polygon[_]) :: Nil => init(r,Some(p))
    case 'reduce :: (bs: List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
    case 'runGroup :: (oldResults: List[_]) :: (bs: List[_]) :: (newResults: List[_]) => {
      val newResults2 = newResults.asInstanceOf[List[List[B]]]
      val results = oldResults.asInstanceOf[List[B]] ::: newResults2.flatten
      bs match {
        case Nil => Result(reducer(results.asInstanceOf[List[B]]))
        case (head: List[_]) :: tail => {
          println("run next batch")
          runAsync('runGroup :: results :: tail :: head)
        }
        case _ => throw new Exception("unexpected state in thoroughput reducer")
      }
    }
  }

  def init(r: Raster, cropPolygon:Option[Polygon[_]]) = {
    r.data match {
      case _: TiledRasterData => {
        val ops = cropPolygon match {
          case None => r.getTileOpList().map(mapper)
          case Some(p) => r.getTileOpList(p).map(mapper)
        }
        val groups = ops grouped (limit) toList
        val tail = groups.tail
        val head = groups.head
        runAsync('runGroup :: List[B]() :: tail :: head)
      }
      case _ => {
        runAsync('runGroup :: List[B]() :: List[B]() :: mapper(r) :: Nil)
      }
    }
  }
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
