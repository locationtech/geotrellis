package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.feature.Point

/**
 * Computes a Density raster based on the Kernel and set of points provided.
 *
 * @param      points           Sequence of point features who's values will be used to
 *                              compute the density.
 * @param      transform        Function that transforms the point feature's data into
 *                              an Int value.
 * @param      kernel           [[Kernel]] to be used in the computation.
 * @param      rasterExtent     Raster extent of the resulting raster.
 *
 * @note                        KernelDensity does not currently support Double raster data.
 *                              If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                              the data values will be rounded to integers.
 */
case class KernelDensity[D](points:Op[Seq[Point[D]]], 
                            transform:Op[D=>Int],
                            kernel:Op[Kernel],
                            rasterExtent:Op[RasterExtent])
     extends Op4(points,transform,kernel,rasterExtent)({ 
       (points,transform,kernel,rasterExtent) =>
         val convolver = new Convolver { }
       
         convolver.initKernel(kernel)
         convolver.init(rasterExtent)
         for(point <- points) {
           val col = convolver.rasterExtent.mapXToGrid(point.geom.getX())
           val row = convolver.rasterExtent.mapYToGrid(point.geom.getY())
           convolver.stampKernel(col,row,transform(point.data))
         }                                                       
         Result(convolver.result)
})
