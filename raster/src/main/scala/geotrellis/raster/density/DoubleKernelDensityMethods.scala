package geotrellis.raster.density

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Kernel
import geotrellis.util.MethodExtensions
import geotrellis.vector._

trait DoubleKernelDensityMethods extends MethodExtensions[Traversable[PointFeature[Double]]] {
  def kernelDensity(kernel: Kernel, rasterExtent: RasterExtent): Tile =
    KernelDensity(self, kernel, rasterExtent)

  def kernelDensity(kernel: Kernel, rasterExtent: RasterExtent, cellType: CellType): Tile =
    KernelDensity(self, kernel, rasterExtent, cellType)
}
