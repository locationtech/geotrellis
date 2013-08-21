package geotrellis.raster

import geotrellis._
import geotrellis.raster.op._
import geotrellis.SourceBuilder
import geotrellis.logic.Collect

object DistributedRasterSource {
  implicit def canBuildSourceFrom: CanBuildSourceFrom[DistributedRasterSource, Raster, DistributedRasterSource] =
    new CanBuildSourceFrom[DistributedRasterSource, Raster, DistributedRasterSource] {
      def apply() = new DistributedRasterSourceBuilder
      def apply(rasterSrc:DistributedRasterSource) =
        DistributedRasterSourceBuilder(rasterSrc)
  }
  
  implicit def canBuildSourceFromHistogram: CanBuildSourceFrom[DistributedRasterSource,statistics.Histogram,DistributedSeqSource[statistics.Histogram]] = new CanBuildSourceFrom[DistributedRasterSource,statistics.Histogram,DistributedSeqSource[statistics.Histogram]] {
    def apply() = new DistributedSeqSourceBuilder[statistics.Histogram]
    def apply(src:DistributedRasterSource) = new DistributedSeqSourceBuilder[statistics.Histogram]
  }

  def apply(name:String):DistributedRasterSource =
    new DistributedRasterSource(
      io.LoadRasterLayerInfo(name).map { info =>
        RasterDefinition(
          info.rasterExtent,
          info.tileLayout,
          (for(tileCol <- 0 until info.tileLayout.tileCols;
            tileRow <- 0 until info.tileLayout.tileRows) yield {
            io.LoadTile(name,tileCol,tileRow)
          }).toSeq
        )
      }
    )
}

class DistributedRasterSource(val rasterDef: Op[RasterDefinition]) extends RasterSource with RasterSourceLike[DistributedRasterSource] {
  def partitions = rasterDef.map(_.tiles)
  val rasterDefinition = rasterDef
}

object Foo {
  def main() = {
    val d1 = new DistributedRasterSource(null)
    val d2: DistributedRasterSource = d1.map(local.Add(_, 3))
    val d3: DistributedRasterSource = d1.localAdd(3)
    val d4: LocalRasterSource = d1.converge

    val l1 = new LocalRasterSource(null)
    val l2: LocalRasterSource = l1.map(local.Add(_, 3))

    val l3: LocalRasterSource = l1.localAdd(2)
  }
}
