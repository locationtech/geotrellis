package geotrellis.source

import geotrellis._
import geotrellis.statistics._

trait CanBuildSourceFrom[-From, Elem, +To] extends AnyRef {
  def apply(): SourceBuilder[Elem, To]
  def apply(from: From): SourceBuilder[Elem, To]
}

object CanBuildSourceFrom  extends LowerPriorityImplicits {
  def convergeHistograms[H <: Histogram](histOps:(Op[Seq[Op[H]]])) = {
    val histograms:Op[Seq[H]] = logic.Collect(histOps)
    val histograms2 = histograms map((hs:Seq[H]) => FastMapHistogram.fromHistograms(hs))
      histograms2
  }

  implicit def canBuildHistogram[H <: Histogram]:CanBuildSourceFrom[DataSource[_,_], H, DataSource[H,Histogram]] = new CanBuildSourceFrom[DataSource[_,_], H, DataSource[H,Histogram]] {
    def apply() = new DataSourceBuilder(convergeHistograms)
    def apply(ds:DataSource[_,_]) = new DataSourceBuilder(convergeHistograms)
  }

  implicit def canBuildRasterSource =  new CanBuildSourceFrom[RasterSource, Raster, RasterSource] {
    def apply() = new RasterSourceBuilder
    def apply(rasterSrc:RasterSource) =
      RasterSourceBuilder(rasterSrc)
  }

  implicit def canBuildValueFromValueSource[E]:CanBuildSourceFrom[ValueDataSource[_], E, ValueDataSource[E]] = new CanBuildSourceFrom[ValueDataSource[_], E, ValueDataSource[E]] {
    def apply() = new ValueDataSourceBuilder[E]()
    def apply(ds:ValueDataSource[_]) = new ValueDataSourceBuilder[E]()
  }
}
/**
  * This trait is necessary to make the implicit builder for Seq[A] lower priority
  * than builders listed for specific types in CanBuildSourceFrom above.
  */
trait LowerPriorityImplicits { this: CanBuildSourceFrom.type =>
  implicit def canBuildSeq[E]:CanBuildSourceFrom[DataSource[_,_], E, DataSource[E,Seq[E]]] = new CanBuildSourceFrom[DataSource[_,_], E, DataSource[E,Seq[E]]] {
    def apply = new DataSourceBuilder(convergeSeq)
    def apply(ds:DataSource[_,_]) = new DataSourceBuilder(convergeSeq)
  }

  def convergeSeq[A](elementOps:(Op[Seq[Op[A]]])) = {
    logic.Collect(elementOps)
  }
}
