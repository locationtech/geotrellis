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

package geotrellis.source

import geotrellis._
import geotrellis.statistics._

trait CanBuildSourceFrom[-From, Elem, +To] extends AnyRef {
  def apply(): SourceBuilder[Elem, To]
  def apply(from: From): SourceBuilder[Elem, To]
}

object CanBuildSourceFrom  extends Priority1Implicits {
  implicit def canBuildRasterSource =  new CanBuildSourceFrom[RasterSource, Raster, RasterSource] {
    def apply() = new RasterSourceBuilder
    def apply(rasterSrc:RasterSource) =
      RasterSourceBuilder(rasterSrc)
  }

  implicit def canBuildRasterFromValue =  new CanBuildSourceFrom[ValueSource[_], Raster, RasterSource] {
    def apply() = new BareRasterSourceBuilder
    def apply(rasterSrc:ValueSource[_]) =
      new BareRasterSourceBuilder
  }
}

trait Priority1Implicits extends Priority2Implicits { this: CanBuildSourceFrom.type =>
  implicit def canBuildValueFromValueSource[E:Manifest]:CanBuildSourceFrom[ValueSource[_], E, ValueSource[E]] = new CanBuildSourceFrom[ValueSource[_], E, ValueSource[E]] {
    def apply() = new ValueSourceBuilder[E]()
    def apply(ds:ValueSource[_]) = new ValueSourceBuilder[E]()
  }

  def convergeHistograms[H <: Histogram](histOps:(Op[Seq[Op[H]]])) = {
    val histograms:Op[Seq[H]] = logic.Collect(histOps)
    val histograms2 = histograms map((hs:Seq[H]) => FastMapHistogram.fromHistograms(hs))
      histograms2
  }

  implicit def canBuildHistogram[H <: Histogram]:CanBuildSourceFrom[DataSource[_,_], H, DataSource[H,Histogram]] = new CanBuildSourceFrom[DataSource[_,_], H, DataSource[H,Histogram]] {
    def apply() = new DataSourceBuilder(convergeHistograms)
    def apply(ds:DataSource[_,_]) = new DataSourceBuilder(convergeHistograms)
  }
}

trait Priority2Implicits { this: CanBuildSourceFrom.type =>
  implicit def canBuildSeq[E]:CanBuildSourceFrom[DataSource[_,_], E, DataSource[E,Seq[E]]] = new CanBuildSourceFrom[DataSource[_,_], E, DataSource[E,Seq[E]]] {
    def apply = new DataSourceBuilder(DataSource.convergeSeq)
    def apply(ds:DataSource[_,_]) = new DataSourceBuilder(DataSource.convergeSeq)
  }
}
