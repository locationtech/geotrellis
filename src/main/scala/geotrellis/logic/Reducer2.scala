package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._
import geotrellis.raster._

abstract class Reducer2[A:Manifest, B:Manifest, C:Manifest](r:Op[Raster], a:Op[A])(handle:(Raster, A) => B)(reducer:(List[B], A) => C) extends Op[C] {
  def _run(context:Context) = runAsync('init :: r :: a :: Nil)

  val nextSteps:Steps = {
    case 'init :: (r:Raster) :: a :: Nil => init(r, a.asInstanceOf[A])
    case 'reduce :: a :: (bs:List[_]) => Result(reducer(bs.asInstanceOf[List[B]], a.asInstanceOf[A]))
  }

  def init(r:Raster, a:A) = {
    r.data match {
      case _:TiledRasterData => runAsync('reduce :: a :: r.getTileOpList.map(t => mapper(t, a)))
      case _ => Result(reducer(handle(r, a) :: Nil, a))
    }
  }

  def mapper(r:Op[Raster], a:Op[A]):Op[B] = logic.Do2(r, a)(handle)
}
