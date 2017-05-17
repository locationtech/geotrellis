package geotrellis.spark.pipeline.function

import geotrellis.raster.{CellType, Tile}

class TileCellTypeChange(target: CellType) extends PipelineFunction[Tile] {
  def apply(v: Tile): Tile = {
    v.convert(target)
  }
}
