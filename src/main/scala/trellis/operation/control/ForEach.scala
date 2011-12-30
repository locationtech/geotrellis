package trellis.operation

import scala.{PartialFunction => PF}
import trellis.process._

case class ForEach[A, Z:Manifest](a:Op[Array[A]], f:(A) => Op[Z]) extends Op[Array[Z]] {
  def childOperations = List(a)

  def _run(server:Server) = runAsync(List(a, server), server)

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (server:Server) :: Nil => step2(as.asInstanceOf[Array[A]], server)
  }

  def step2(as:Array[A], server:Server) = Some(as.map(a => server.run(f(a))).toArray)
}

case class ForEach2[A, B, Z:Manifest](a:Op[Array[A]],
                                      b:Op[Array[B]],
                                      f:(A, B) => Op[Z]) extends Op[Array[Z]] {
  def childOperations = List(a, b)

  def _run(server:Server) = runAsync(List(a, b, server), server)

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (bs:Array[_]) :: (server:Server) :: Nil => {
      step2(as.asInstanceOf[Array[A]],
            bs.asInstanceOf[Array[B]],
            server)
    }
    case zs:List[_] => Some(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B], server:Server) = {
    val ops = (0 until as.length).map(i => f(as(i), bs(i))).toList
    runAsync(ops, server)
  }
}

case class ForEach3[A, B, C, Z:Manifest](a:Op[Array[A]],
                                         b:Op[Array[B]],
                                         c:Op[Array[C]],
                                         f:(A, B, C) => Op[Z])
extends Op[Array[Z]] {

  def childOperations = List(a, b, c)

  def _run(server:Server) = runAsync(List(a, b, c, server), server)

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (bs:Array[_]) :: (cs:Array[_]) :: (server:Server) :: Nil => {
      step2(as.asInstanceOf[Array[A]],
            bs.asInstanceOf[Array[B]],
            cs.asInstanceOf[Array[C]],
            server)
      
    }
    case zs:List[_] => {
      Some(zs.asInstanceOf[List[Z]].toArray)
    }
  }

  def step2(as:Array[A], bs:Array[B], cs:Array[C], server:Server) = {
    val ops = (0 until as.length).map(i => f(as(i), bs(i), cs(i))).toList
    runAsync(ops, server)
  }
}
