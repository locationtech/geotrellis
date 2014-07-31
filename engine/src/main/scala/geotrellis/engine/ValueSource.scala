/*
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
 */

package geotrellis.engine

class ValueSource[+T](val element:Op[T]) extends OpSource[T] {
  def convergeOp() = element

  def map[B](f: T => B): ValueSource[B] = mapOp(Op(f(_)))
  def map[B](f: T => B, name: String): ValueSource[B] = mapOp(_.map(f).withName(name))
  def mapOp[B](f: Op[T] => Op[B]): ValueSource[B] = ValueSource(f(element))

  def combine[B,C](os: OpSource[B])(f: (T, B) => C): ValueSource[C] =
    combineOp(os) { (a, b) => (a, b).map(f(_, _)) }
  def combine[B,C](os: OpSource[B], name: String)(f: (T, B) => C): ValueSource[C] =
    combineOp(os) { (a, b) => (a, b).map(f(_, _)).withName(name) }
  def combineOp[B,C](os: OpSource[B])(f: (Op[T], Op[B]) => Op[C]): ValueSource[C] =
    ValueSource(f(convergeOp,os.convergeOp))
}

object ValueSource {
  def apply[T](element:Op[T]):ValueSource[T] = new ValueSource(element)
 }
