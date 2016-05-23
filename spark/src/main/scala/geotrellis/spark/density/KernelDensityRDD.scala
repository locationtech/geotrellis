/*
 * Copyright (c) 2016 Azavea.
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
 */

package geotrellis.spark.density

import geotrellis.spark._
import geotrellis.spark.tiling._

import org.apache.spark.rdd.RDD

import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.mapalgebra.local._
import geotrellis.raster.density._

object KernelDensityRDD {

  def kernelDensity(rdd: RDD[PointFeature[Double]], ld: LayoutDefinition, kern: Kernel, crs: CRS): RDD[(SpatialKey,Tile)] with Metadata[TileLayerMetadata[SpatialKey]] = {
    val kw = 2 * kern.extent.toDouble + 1.0
    val tl = ld.tileLayout
    
    def ptfToExtent[D](ptf: PointFeature[D]): Extent = {
      val p = ptf.geom
      Extent(p.x - kw * ld.cellwidth / 2,
             p.y - kw * ld.cellheight / 2,
             p.x + kw * ld.cellwidth / 2,
             p.y + kw * ld.cellheight / 2)
    }

    val trans = (_.toFloat.round.toInt): Double => Int

    def ptfToSpatialKey[D](ptf: PointFeature[D]): Seq[(SpatialKey, PointFeature[D])] = {
      val ptextent = ptfToExtent(ptf)
      val gridBounds = ld.mapTransform(ptextent)
      for ((c,r) <- gridBounds.coords;
           if r < tl.totalRows;
           if c < tl.totalCols) yield (SpatialKey(c,r), ptf)
    }

    val keyfeatures = rdd.flatMap(ptfToSpatialKey).groupByKey.mapValues(_.toList)

    val keytiles: RDD[(SpatialKey, Tile)] = keyfeatures.map { 
      case (sk,pfs) => (sk,KernelDensity.kernelDensity(pfs,trans,kern,
                                        RasterExtent(ld.mapTransform(sk),
                                                     tl.tileDimensions._1,
                                                     tl.tileDimensions._2)))
    }

    val metadata = TileLayerMetadata(DoubleCellType,
                                     ld,
                                     ld.extent,
                                     crs,
                                     KeyBounds(SpatialKey(0,0),
                                               SpatialKey(ld.layoutCols-1,
                                                          ld.layoutRows-1)))
    ContextRDD(keytiles,metadata)
  }

}
