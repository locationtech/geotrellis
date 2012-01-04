package trellis.operation

import scala.{PartialFunction => PF}
import trellis.process._

case class ForEach[A, Z:Manifest](a:Op[Array[A]], f:(A) => Op[Z]) extends Op[Array[Z]] {
  def _run(context:Context) = runAsync(List(a, context))

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (context:Context) :: Nil => step2(as.asInstanceOf[Array[A]], context)
  }

  def step2(as:Array[A], context:Context) = Result(as.map(a => context.run(f(a))).toArray)
}

case class ForEach2[A, B, Z:Manifest](a:Op[Array[A]],
                                      b:Op[Array[B]],
                                      f:(A, B) => Op[Z]) extends Op[Array[Z]] {

  def _run(context:Context) = runAsync(List(a, b, context))

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (bs:Array[_]) :: (context:Context) :: Nil => {
      step2(as.asInstanceOf[Array[A]],
            bs.asInstanceOf[Array[B]],
            context)
    }
    case zs:List[_] => Result(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B], context:Context) = {
    val ops = (0 until as.length).map(i => f(as(i), bs(i))).toList
    runAsync(ops)
  }
}

case class ForEach3[A, B, C, Z:Manifest](a:Op[Array[A]],
                                         b:Op[Array[B]],
                                         c:Op[Array[C]],
                                         f:(A, B, C) => Op[Z])
extends Op[Array[Z]] {

  def _run(context:Context) = runAsync(List(a, b, c, context))

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (bs:Array[_]) :: (cs:Array[_]) :: (context:Context) :: Nil => {
      step2(as.asInstanceOf[Array[A]],
            bs.asInstanceOf[Array[B]],
            cs.asInstanceOf[Array[C]],
            context)
      
    }
    case zs:List[_] => Result(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B], cs:Array[C], context:Context) = {
    val ops = (0 until as.length).map(i => f(as(i), bs(i), cs(i))).toList
    runAsync(ops)
  }
}
