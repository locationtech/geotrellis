/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.util

class KryoClosure1[T1, R](f: T1=>R) extends KryoWrapper[T1=>R] with (T1=>R) {
  assert(f != null)
  value = f

  def apply(in: T1): R = {
    assert(value != null)
    value.apply(in)
  }
}
class KryoClosure2[T1, T2, R](f: (T1,T2)=>R) extends KryoWrapper[(T1, T2)=>R] with ((T1,T2)=>R) {
  assert(f != null)
  value = f

  def apply(t1: T1, t2: T2): R = {
    assert(value != null)
    value.apply(t1,t2)
  }
}

object KryoClosure {
  // import com.esotericsoftware.minlog.Log
  // import com.esotericsoftware.minlog.Log._
  // Log.set(LEVEL_TRACE)

  def apply[T1, R](f: T1 => R)  = new KryoClosure1[T1, R](f)
  def apply[T1, T2, R](f: (T1,T2) => R)  = new KryoClosure2[T1, T2, R](f)
}
