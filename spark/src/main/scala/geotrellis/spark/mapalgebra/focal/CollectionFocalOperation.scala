package geotrellis.spark.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.spark._
import geotrellis.spark.buffer.BufferedTile
import geotrellis.util.MethodExtensions

object CollectionFocalOperation {
  private def mapOverBufferedTiles[K: SpatialComponent](bufferedTiles: Seq[(K, BufferedTile[Tile])], neighborhood: Neighborhood)
                                                       (calc: (Tile, Option[GridBounds]) => Tile): Seq[(K, Tile)] =
    bufferedTiles
      .map { case (key, BufferedTile(tile, gridBounds)) => key -> calc(tile, Some(gridBounds)) }

  def apply[K: SpatialComponent](seq: Seq[(K, Tile)], neighborhood: Neighborhood)
                                (calc: (Tile, Option[GridBounds]) => Tile)(implicit d: DummyImplicit): Seq[(K, Tile)] =
    mapOverBufferedTiles(seq.bufferTiles(neighborhood.extent), neighborhood)(calc)

  def apply[K: SpatialComponent](rdd: Seq[(K, Tile)], neighborhood: Neighborhood, layerBounds: GridBounds)
                                (calc: (Tile, Option[GridBounds]) => Tile): Seq[(K, Tile)] =
    mapOverBufferedTiles(rdd.bufferTiles(neighborhood.extent, layerBounds), neighborhood)(calc)

  def apply[K: SpatialComponent](rasterCollection: TileLayerCollection[K], neighborhood: Neighborhood)
                                          (calc: (Tile, Option[GridBounds]) => Tile): TileLayerCollection[K] =
    rasterCollection.withContext { rdd =>
      apply(rdd, neighborhood, rasterCollection.metadata.gridBounds)(calc)
    }
}

abstract class CollectionFocalOperation[K: SpatialComponent] extends MethodExtensions[TileLayerCollection[K]] {

  def focal(n: Neighborhood)
      (calc: (Tile, Option[GridBounds]) => Tile): TileLayerCollection[K] =
    CollectionFocalOperation(self, n)(calc)

  def focalWithCellSize(n: Neighborhood)
      (calc: (Tile, Option[GridBounds], CellSize) => Tile): TileLayerCollection[K] = {
    val cellSize = self.metadata.layout.cellSize
    CollectionFocalOperation(self, n){ (tile, bounds) =>
      calc(tile, bounds, cellSize)
    }
  }
}
