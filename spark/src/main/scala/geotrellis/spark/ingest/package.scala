package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster.mosaic.MergeView
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.vector._
import geotrellis.raster._
import org.apache.spark.rdd._
import monocle.syntax._


package object ingest {
  type Tiler[T, K, TileType] = (RDD[(T, TileType)], RasterMetaData, ResampleMethod) => RDD[(K, TileType)]
  type IngestKey[T] = KeyComponent[T, ProjectedExtent]

  implicit class IngestKeyWrapper[T: IngestKey](key: T) {
    val _projectedExtent = implicitly[IngestKey[T]]

    def projectedExtent: ProjectedExtent = key &|-> _projectedExtent.lens get

    def updateProjectedExtent(pe: ProjectedExtent): T =
      key &|-> _projectedExtent.lens set (pe)
  }

  implicit object ProjectedExtentComponent extends IdentityComponent[ProjectedExtent]

  implicit def projectedExtentToSpatialKeyTiler: Tiler[ProjectedExtent, SpatialKey, Tile] = {
    val getExtent = (inKey: ProjectedExtent) => inKey.extent
    val createKey = (inKey: ProjectedExtent, spatialComponent: SpatialKey) => spatialComponent
    Tiler(getExtent, createKey)
  }

  implicit def projectedExtentToSpatialKeyMultiBandTiler: Tiler[ProjectedExtent, SpatialKey, MultiBandTile] = {
    val getExtent = (inKey: ProjectedExtent) => inKey.extent
    val createKey = (inKey: ProjectedExtent, spatialComponent: SpatialKey) => spatialComponent
    Tiler(getExtent, createKey)
  }


  type CellGridPrototypeView[TileType] = TileType => CellGridPrototype[TileType]

  implicit class withTilePrototypeMethods(tile: Tile) extends CellGridPrototype[Tile] {
    def prototype(cellType: CellType, cols: Int, rows: Int) =
      ArrayTile.empty(cellType, cols, rows)

    def prototype(cols: Int, rows: Int) =
      prototype(tile.cellType, cols, rows)

  }

  implicit class withMultiBandTilePrototype(tile: MultiBandTile) extends CellGridPrototype[MultiBandTile] {
    def prototype(cellType: CellType, cols: Int, rows: Int) =
      ArrayMultiBandTile.empty(cellType, tile.bandCount, cols, rows)

    def prototype(cols: Int, rows: Int) =
      prototype(tile.cellType, cols, rows)
  }

  implicit class withCollectMetadataMethods[K: IngestKey, TileType <: CellGrid](rdd: RDD[(K, TileType)]) {
    def collectMetaData(crs: CRS, layoutScheme: LayoutScheme): (Int, RasterMetaData) = {
      RasterMetaData.fromRdd(rdd, crs, layoutScheme)(_.projectedExtent.extent)
    }

    def collectMetaData(crs: CRS, layout: LayoutDefinition): RasterMetaData = {
      RasterMetaData.fromRdd(rdd, crs, layout)(_.projectedExtent.extent)
    }
  }

  implicit class withTilerMethods[T, K, TileType](tiles: RDD[(T, TileType)]) {
    def tile(rasterMetaData: RasterMetaData, resampleMethod: ResampleMethod = NearestNeighbor)(implicit tiler: Tiler[T, K, TileType]): RDD[(K, TileType)] = {
      tiler(tiles, rasterMetaData, resampleMethod)
    }
  }

  implicit class withRddMergeMethods[K, TileType: MergeView](rdd: RDD[(K, TileType)])
    extends RddMergeMethods[K, TileType](rdd)

  implicit class withRddLayoutMergeMethods[K, TileType: MergeView](rdd: (RDD[(K, TileType)], LayoutDefinition))
    extends RddLayoutMergeMethods[K, TileType](rdd)

  implicit class withRasterRddMergeMethods[K, TileType: MergeView](rdd: RasterRDD[K])
    extends RasterRddMergeMethods[K](rdd)

  implicit class withMultiBandRasterRddMergeMethods[K, TileType: MergeView](rdd: MultiBandRasterRDD[K])
    extends MultiBandRasterRddMergeMethods[K](rdd)
}
