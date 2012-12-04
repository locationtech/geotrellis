package geotrellis.raster.op.focal

import geotrellis._
import scala.math.{max,min}

case class Gaussian(size: Op[Int], cellWidth: Op[Double], sigma: Op[Double], amp: Op[Double]) 
     extends Op4[Int,Double,Double,Double,Raster](size, cellWidth, sigma, amp)((n,w,sigma,amp) => {
       val extent = Extent(0,0,n*w,n*w)
       val rasterExtent = RasterExtent(extent, w, w, n, n)
       val outputR = Raster.empty(rasterExtent)
       val output = outputR.data.mutable.get

       var r = 0
       var c = 0
       while(r < n) {
         while(c < n) {
           val rsqr = (c - n/2)*(c - n/2) + (r - n/2)*(r - n/2)
           val g = (amp * (math.exp(-rsqr / (2.0*sigma*sigma)))).toInt
           output.set(c,r,g)

           c += 1
         }
         c = 0
         r += 1
       }

       Result(outputR)
     })

case class Convolve(raster: Op[Raster], kernel: Op[Raster]) 
     extends Op2[Raster, Raster, Raster](raster, kernel)((raster, kernel) => {

       val e = raster.rasterExtent
       val outputR = Raster.empty(e);
       val output = outputR.data.mutable.get

       var r = 0
       var c = 0
       var r2 = 0 
       var c2 = 0 
       var kc = 0 
       var kr = 0

       val rows = raster.rows
       val cols = raster.cols

       val rowsk = kernel.rows
       val colsk = kernel.cols

       while(r < rows) {
         while(c < cols) {
           val z = raster.get(c,r)

           if (z != NODATA && z != 0) {
             r2 = r - rowsk / 2
             val c2base = c - colsk / 2
             c2 = c2base
             kc = 0
             kr = 0

             val maxr = math.min(r + rowsk / 2 + 1,rows)
             val maxc = math.min(c + colsk / 2 + 1,cols)

             while(r2 < maxr) {
               while(c2 < maxc) {               
                 if (r2 >= 0 && c2 >= 0 && r2 < e.rows && c2 < e.cols &&
                     kc >= 0 && kr >= 0 && kc < colsk && kr < rowsk) {
                   val k = kernel.get(kc,kr)
                   if (k != NODATA) {
                     val o = output.get(c2,r2)
                     val w = if (o == NODATA) {
                       k * z
                     } else {
                       o + k*z
                     }
                     
                     output.set(c2,r2,w)
                   }
                 } 

                 c2 += 1
                 kc += 1
               }
               kc = 0
               c2 = c2base
               r2 += 1
               kr += 1
             }
           }

           c += 1           
         }
         c = 0
         r += 1
       }


       Result(outputR)
     }) 

