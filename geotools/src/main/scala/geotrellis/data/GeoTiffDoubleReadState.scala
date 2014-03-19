/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.data

import geotrellis._
import geotrellis.raster._

import org.geotools.gce

class GeoTiffDoubleReadState(path:String,
                          val rasterExtent:RasterExtent,
                          val target:RasterExtent,
                          val typ:RasterType,
                          val reader:gce.geotiff.GeoTiffReader) extends ReadState {
  def getType = typ

  private var noData:Double = 0.0
  private var data:Array[Double] = null
  
  private def initializeNoData(reader:gce.geotiff.GeoTiffReader) = {
    noData = reader.getMetadata.getNoData.toDouble
  }

  def getNoDataValue = noData

  def initSource(pos:Int, size:Int) {
    val x = 0
    val y = pos / rasterExtent.cols
    val w = rasterExtent.cols
    val h = size / rasterExtent.cols

    initializeNoData(reader)
    data = Array.fill(w * h)(noData)
    val geoRaster = reader.read(null).getRenderedImage.getData
    geoRaster.getPixels(x, y, w, h, data)
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, data(sourceIndex))
  }

  protected[this] override def translate(rData:MutableRasterData) {
    if(noData != Double.NaN) {
      println(s"NoData value is $noData, converting to NaN")
      var i = 0
      val len = rData.length
      var conflicts = 0
      while (i < len) {
        if(rData(i) == Double.NaN) conflicts += 1
        if (rData(i) == noData) rData.updateDouble(i, Double.NaN)
        i += 1
      }
      if(conflicts > 0) {
        println(s"[WARNING]  GeoTiff contained values of ${Double.NaN}, which are considered to be NO DATA values in ARG format. There are $conflicts raster cells that are now considered NO DATA values in the converted format.")
      }
    }
  }
}
