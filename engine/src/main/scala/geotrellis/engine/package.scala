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
package geotrellis

import geotrellis.raster.CanBuildSourceFrom
import geotrellis.raster.stats.Histogram

import geotrellis.raster._

import language.experimental.macros

package object engine {
  implicit lazy val engine = GeoTrellis.engine

  type Callback[+T] = (List[Any]) => StepOutput[T]
  type Args = List[Any]

  type Op[+A] = Operation[A]
  type DI = DummyImplicit

  /**
   * Syntax for converting tuples of operations
   * into objects that you can call map and flatMap on.
   * this is similiar to a for comprehension, but the
   * operations will be executed in parallel.
   */
  implicit class OpMap2[A, B](t: (Op[A], Op[B])) {
    def map[T](f: (A, B)=>T) = Op(f).apply(t._1, t._2)
    def flatMap[T](f: (A, B)=>Op[T]) = Op(f).apply(t._1, t._2)
  }

  implicit class OpMap3[A, B, C](t: (Op[A], Op[B], Op[C])) {
    def map[T](f: (A, B, C)=>T) = Op(f).apply(t._1, t._2, t._3)
    def flatMap[T](f: (A, B, C)=>Op[T]) = Op(f).apply(t._1, t._2, t._3)
  }

  implicit class OpMap4[A, B, C, D](t: (Op[A], Op[B], Op[C], Op[D])) {
    def map[T](f: (A, B, C, D)=>T) = Op(f).apply(t._1, t._2, t._3, t._4)
    def flatMap[T](f: (A, B, C, D)=>Op[T]) = Op(f).apply(t._1, t._2, t._3, t._4)
  }

  /** 
   * Syntax for converting an iterable collection to 
   * have methods to work with the results of those 
   * operations executed in parallel
   */
  implicit class OpMapSeq[A](seq: Seq[Op[A]]) {
    def mapOps[T](f: (Seq[A]=>T)) = logic.Collect(Literal(seq)).map(f)
    def flaMapOps[T](f: (Seq[A]=>Op[T])) = logic.Collect(Literal(seq)).flatMap(f)
  }

  implicit class OpMapArray[A](seq: Array[Op[A]]) {
    def mapOps[T](f: (Seq[A]=>T)) = logic.Collect(Literal(seq.toSeq)).map(f)
    def flaMapOps[T](f: (Seq[A]=>Op[T])) = logic.Collect(Literal(seq.toSeq)).flatMap(f)
  }

  implicit class OpSeqToCollect[T](seq: Op[Seq[Op[T]]]) {
    def collect() = logic.Collect(seq)
  }

  type SeqSource[+T] = DataSource[T, Seq[T]]
  type HistogramSource = DataSource[Histogram, Histogram]

  implicit def iterableRasterSourceToRasterSourceSeq(iterable: Iterable[RasterSource]): RasterSourceSeq =
    RasterSourceSeq(iterable.toSeq)

  implicit def dataSourceSeqToSeqSource[T](iterable: Iterable[DataSource[_, T]]): SeqSource[T] =
    DataSource.fromSources(iterable.toSeq)

  implicit class DataSourceSeqWrapper[T](dss: Seq[DataSource[_, T]]) {
    def collectSources(): SeqSource[T] = DataSource.fromSources(dss)
  }
}
