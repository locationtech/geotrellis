package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.statistics._
import scala.math._
import geotrellis.raster._

/**
 * Here is a MultiplyConstant implementation in terms of a re-imaginined
 * UnaryLocal/WithInt implementation. This implementation is very similar to
 * the implementation of MultiplyConstantMapIfSet.
 *
 * NOTE: I'm worried that multiple subclasses of CustomWithInt might trigger
 * de-inlining, which could hurt performance in practice. But at this point
 * that concern is mostly superstitious.
 */
abstract class CustomWithInt(r:Op[Raster], c:Op[Int]) extends Op[Raster] {
  final def _run(context:Context) = runAsync(r :: c :: Nil)

  def handleCell(z:Int, n:Int): Int

  final val nextSteps:Steps = {
    case (raster:Raster) :: (n:Int) :: Nil => Result(raster.mapIfSet(z => handleCell(z, n)))
  }
}
case class MultiplyConstantCustomWithInt(r:Op[Raster], c:Op[Int]) extends CustomWithInt(r, c) {
  @inline final def handleCell(z:Int, n:Int) = z * n
}

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet.
 */
case class MultiplyConstantMapIfSet(r:Op[Raster], c:Op[Int]) extends Op[Raster] {
  def _run(context:Context) = runAsync(r :: c :: Nil)

  final def _finish(raster:Raster, n:Int) = raster.mapIfSet(_ * n)

  val nextSteps:Steps = {
    case (raster:Raster) :: (n:Int) :: Nil => Result(_finish(raster, n))
  }
}

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet and Op2.
 */
case class MultiplyConstantMapIfSetSugar(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.mapIfSet(_ * c))
})

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet and Op2.
 */
case class MultiplyConstantMapSugar(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.map(z => if (isData(z)) z * c else NODATA))
})

/**
 * Here is a MultiplyConstant implementation in terms of raster.mapIfSet and Op2.
 */
case class MultiplyConstantMapIfSetSugarWithLiteral(r:Op[Raster], c:Int) extends Op1(r)({
  r => Result(r.mapIfSet(_ * c))
})

/**
 * Here is a MultiplyConstant implementation in terms of a while-loop.
 */
case class MultiplyConstantWhileLoop(r:Op[Raster], c:Op[Int]) extends Op[Raster] {
  def _run(context:Context) = runAsync(r :: c :: Nil)

  val nextSteps:Steps = {
    case (raster:Raster) :: (n:Int) :: Nil => {
      val r2 = raster.toArrayRaster
      val data = r2.data.mutable

      val len = r2.cols * r2.rows
      var i = 0
      while (i < len) {
        val z = data(i)
        if (isData(z)) data(i) = z * n
        i += 1
      }
      Result(r2)
    }
  }
}

case class UntiledMin(r:Op[Raster]) extends Operation[Int] {
  def _run(context:Context) = runAsync(r :: Nil)
  val nextSteps:Steps = {
    case (raster:Raster) :: Nil => {
      var zmin = Int.MaxValue 
      raster.foreach {
        data => {
          z:Int => if (isData(z)) {
            zmin = min(zmin, z)
          }
        }
      }
      Result(zmin)
    }
  }
}

/**
 * older version of the MultiLocal trait
 */
trait MultiLocalOld extends Operation[Raster]{
  def ops:Array[Op[Raster]]

  def _run(context:Context) = runAsync(ops.toList)

  val nextSteps:Steps = {
    case rasters:List[_] => {
      handleRasters(rasters.asInstanceOf[List[Raster]].toArray)
    }
  }

  def handle(z1:Int, z2:Int):Int

  def handleRaster(_outdata:RasterData, _data:RasterData) {
    val data = _data.toArray
    val outdata = _outdata.mutable
    var i = 0
    while (i < outdata.length) {
      outdata(i) = handle(outdata(i), data(i))
      i += 1
    }
  }
  
  def handleRasters(rasters:Array[Raster]) = {
    val output = rasters(0).toArrayRaster
    val outdata = output.data
    var j = 0
    while (j < rasters.length) {
      handleRaster(outdata, rasters(j).toArrayRaster.data)
      j += 1
    }
    Result(output)
  }
}

/**
 * older version of Add, based on MultiLocalOld
 */
case class AddOld(rs:Op[Raster]*) extends MultiLocalOld {
  final def ops = rs.toArray
  final def handle(a:Int, b:Int) = if (isNoData(a)) b else if (isNoData(b)) a else a + b
}

// the winner!
// case class BTileForceHistogram(r:Op[Raster]) extends BReducer1(r)({
//   r => FastMapHistogram.fromRaster(r.force)
// })({
//   hs => FastMapHistogram.fromHistograms(hs)
// })


/**
 *
 */
// case class BTileHistogram(r:Op[Raster]) extends BReducer1(r)({
//   r => FastMapHistogram.fromRaster(r)
// })({
//   hs => FastMapHistogram.fromHistograms(hs)
// })

case class BUntiledHistogram(r:Op[Raster]) extends Op1(r) ({
  r => {
    Result( FastMapHistogram.fromHistograms(FastMapHistogram.fromRaster(r):: Nil))
  }
})

// case class BTileMin(r:Op[Raster]) extends BReducer1(r)({
//   r => {
//     var zmin = Int.MaxValue
//     r.foreach {
//       z => if (isData(z)) zmin = min(z,zmin)
//     }
//     zmin
//   }
// })({
//   zs => zs.reduceLeft((x,y) => min(x,y))
// })

// abstract class BReducer1[B:Manifest, C:Manifest](r:Op[Raster])(handle:Raster => B)(reducer:List[B] => C) extends Op[C] {
//   def _run(context:Context) = runAsync('init :: r :: Nil)

//   val nextSteps:Steps = {
//     case 'init :: (r:Raster) :: Nil => init(r)
//     case 'reduce :: (bs:List[_]) => Result(reducer(bs.asInstanceOf[List[B]]))
//   }

//   def init(r:Raster) = {
//     r.data match {
//       case _:TiledRasterData => runAsync('reduce :: r.getTileOpList.map(mapper))
//       case _ => Result(reducer(handle(r) :: Nil))
//     }
//   }

//   def mapper(r:Op[Raster]):Op[B] = logic.Do(r)(handle)
// }
