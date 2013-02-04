package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._
import geotrellis.raster._

abstract class Reducer1[B:Manifest, C:Manifest](r:Op[Raster])
                                               (handle:Raster => B)
                                               (reducer:List[B] => C) 
         extends Op[C] {
  def _run(context:Context) = runAsync('init :: r :: Nil)

  val nextSteps:Steps = {
    case 'init :: (r:Raster) :: Nil => init(r)
    case 'reduce :: (bs:List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
  }

  def init(r:Raster) = {
    r.data match {
      case _:TiledRasterData => runAsync('reduce :: r.getTileOpList.map(mapper))
      case _ => Result(reducer(handle(r) :: Nil))
    }
  }

  def mapper(r:Op[Raster]):Op[B] = logic.Do(r)(handle)
}
