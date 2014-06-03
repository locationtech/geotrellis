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

package geotrellis.source

import geotrellis._

/**
 * Represents a data source that may be distributed across machines (logical data source) 
 * or loaded in memory on a specific machine. 
  */
trait DataSource[+T,+V] extends DataSourceLike[T,V,DataSource[T,V]] {
}

object DataSource {
  def convergeSeq[A](elementOps:(Op[Seq[Op[A]]])) = {
    logic.Collect(elementOps)
  }

  def fromValues[T](elements:T*):SeqSource[T] =
    fromValues(elements)

  def fromValues[T](elements:Seq[T])(implicit d:DI): SeqSource[T] =
    apply(Literal(elements.map(Literal(_))))

  def fromSources[T](sources: Seq[DataSource[_,T]]): SeqSource[T] =
    apply(Literal(sources.map(_.convergeOp)))

  def apply[T](elements:Op[Seq[Op[T]]]): SeqSource[T] = {
    val builder:DataSourceBuilder[T,Seq[T]] = new DataSourceBuilder(convergeSeq)
    builder.setOp(elements)
    builder.result
  }
}
