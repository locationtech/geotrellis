package geotrellis.spark.density

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Kernel
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util.MethodExtensions
import geotrellis.vector._

import org.apache.spark.rdd.RDD

trait RDDDoubleKernelDensityMethods extends MethodExtensions[RDD[PointFeature[Double]]] {
  def kernelDensity(kernel: Kernel, layoutDefinition: LayoutDefinition, crs: CRS): RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] =
    RDDKernelDensity(self, layoutDefinition, kernel, crs)

  def kernelDensity(kernel: Kernel, layoutDefinition: LayoutDefinition, crs: CRS, cellType: CellType): RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] =
    RDDKernelDensity(self, layoutDefinition, kernel, crs, cellType)
}
