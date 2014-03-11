/***
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._

case class CollectMap[K,V](map:Op[Map[K,Op[V]]]) extends Op[Map[K,V]] {
  def _run() = runAsync(List('init, map)) 
  val nextSteps:Steps = {
    case 'init :: (m:Map[_,_]) :: Nil => 
      val opMap = m.asInstanceOf[Map[K,Op[V]]]
      val l:List[Op[(K,V)]] = opMap.map { case (k,v) => v.map((k,_)) }.toList
      runAsync('eval :: l)
    case 'eval :: (l:List[_]) => Result(l.asInstanceOf[List[(K,V)]].toMap) 
    case _ => throw new Exception("unexpected stepresult")
  }
}

object Collect {
  def apply[K,V](m:Op[Map[K,Op[V]]]) = 
    CollectMap(m)
}

/**
 * Takes a sequence of operations, and returns a Sequence of the results of those operations.
 */
case class Collect[A](ops:Op[Seq[Op[A]]]) extends Op[Seq[A]] {
  def _run() = runAsync(List('init, ops)) 
  val nextSteps:Steps = {
    case 'init :: (opSeq:Seq[_]) :: Nil => runAsync('eval :: opSeq.toList)
    case 'eval :: (as:List[_]) => Result(as.asInstanceOf[List[A]].toSeq) 
    case _ => throw new Exception("unexpected stepresult")
  }
}

object CollectArray {
  @deprecated("Use Collect.","0.9")
  def apply[A:Manifest](ops:Array[Op[A]]):Op[Array[A]] = Collect(Literal(ops.toSeq)).map( _.toArray ) 
}
