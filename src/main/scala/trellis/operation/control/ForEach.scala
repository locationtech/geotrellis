package trellis.operation

import trellis.process.{Server, Results}

case class ForEach[A, Z:Manifest](a:Op[Array[A]], f:(A) => Op[Z]) extends Op[Array[Z]] {
  def childOperations = List(a)

  def _run(server:Server, cb:Callback) = runAsync(List(a, server), server, cb)

  val nextSteps:Steps = {
    case Results(List(as:Array[_], server:Server)) => step2(as.asInstanceOf[Array[A]], server)
  }

  def step2(as:Array[A], server:Server) = Some(as.map(a => server.run(f(a))).toArray)
}

case class ForEach2[A, B, Z:Manifest](a:Op[Array[A]],
                                      b:Op[Array[B]],
                                      f:(A, B) => Op[Z]) extends Op[Array[Z]] {
  def childOperations = List(a, b)

  def _run(server:Server, cb:Callback) = runAsync(List(a, b, server, cb), server, cb)

  val nextSteps:Steps = {
    case Results(List(as:Array[_], bs:Array[_], server:Server, cb:Function1[_, _])) => step2(
      as.asInstanceOf[Array[A]],
      bs.asInstanceOf[Array[B]],
      server,
      cb.asInstanceOf[Function1[Option[Array[Z]], Unit]]
    )
    case Results(zs:List[_]) => Some(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B], server:Server, cb:Callback) = {
    val ops = (0 until as.length).map(i => f(as(i), bs(i))).toList
    runAsync(ops, server, cb)
  }
}

case class ForEach3[A, B, C, Z:Manifest](a:Op[Array[A]],
                                         b:Op[Array[B]],
                                         c:Op[Array[C]],
                                         f:(A, B, C) => Op[Z])
extends Op[Array[Z]] {

  def childOperations = List(a, b, c)

  def _run(server:Server, cb:Callback) = runAsync(List(a, b, c, server, cb), server, cb)

  val nextSteps:Steps = {
    case Results(List(as:Array[_], bs:Array[_], cs:Array[_], server:Server, cb:Function1[_, _])) => step2(
      as.asInstanceOf[Array[A]],
      bs.asInstanceOf[Array[B]],
      cs.asInstanceOf[Array[C]],
      server,
      cb.asInstanceOf[Function1[Option[Array[Z]], Unit]]
    )
    case Results(zs:List[_]) => Some(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B], cs:Array[C], server:Server, cb:Callback) = {
    val ops = (0 until as.length).map(i => f(as(i), bs(i), cs(i))).toList
    runAsync(ops, server, cb)
  }
}
