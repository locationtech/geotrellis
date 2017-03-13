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

package geotrellis.spark.etl

import scala.reflect.runtime.universe._

trait TypedModule {
  var map: Map[TypeTag[_], () => _] = Map.empty

  def register[T: TypeTag](thing: => T): Unit =
    map = map updated (typeTag[T], () => thing)

  def find[T: TypeTag]: Option[T] =
    map
      .get(typeTag[T])
      .map(_.apply())
      .asInstanceOf[Option[T]]

  def findSubclassOf[T: TypeTag]: Seq[T] = {
    val target = typeTag[T]
    map
      .filterKeys( tt => tt.tpe <:< target.tpe)
      .values
      .map(_.apply())
      .toSeq
      .asInstanceOf[Seq[T]]
  }

  def union(other: TypedModule): TypedModule = {
    val tm = new TypedModule{}
    tm.map = map ++ other.map
    tm
  }
}
