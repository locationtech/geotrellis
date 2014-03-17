/**************************************************************************
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
 **************************************************************************/

package geotrellis.util

/**
  * Utility class for timing the execution time of a function.
  */
object Timer {
  def time[T](thunk: => T) = {
    val t0 = System.currentTimeMillis()
    val result = thunk
    val t1 = System.currentTimeMillis()
    (result, t1 - t0)
  }

  def run[T](thunk: => T) = {
    val (result, t) = time { thunk }
    printf("%s: %d ms\n", result, t)
    result
  }

  def log[T](fmt:String, args:Any*)(thunk: => T) = {
    val label = fmt.format(args:_*)
    val (result, t) = time { thunk }
    printf(label + ": %d ms\n".format(t))
    result
  }
}
