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

package object source {
  type SeqSource[+T] = DataSource[T,Seq[T]]
  type HistogramSource = DataSource[statistics.Histogram,statistics.Histogram]

  implicit def iterableRasterSourceToRasterSourceSeq(iterable:Iterable[RasterSource]):RasterSourceSeq =
    RasterSourceSeq(iterable.toSeq)

  implicit def dataSourceSeqToSeqSource[T](iterable:Iterable[DataSource[_,T]]): SeqSource[T] =
    DataSource.fromSources(iterable.toSeq)

  implicit class DataSourceSeqWrapper[T](dss: Seq[DataSource[_,T]]) {
    def collectSources():SeqSource[T] = DataSource.fromSources(dss)
  }
}
