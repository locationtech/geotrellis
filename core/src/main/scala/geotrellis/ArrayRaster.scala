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

package geotrellis

import geotrellis.raster._

import spire.syntax.cfor._

case class ArrayRaster(data: RasterData, rasterExtent: RasterExtent) extends Raster {
  val rasterType = data.getType

  def toArrayRaster = this
  def toArray = data.toArray
  def toArrayDouble = data.toArrayDouble
  def toArrayByte = data.toArrayByte

  def get(col:Int, row:Int):Int = data.get(col, row)
  def getDouble(col:Int, row:Int):Double = data.getDouble(col, row)

  def convert(typ:RasterType) = Raster(data.convert(typ), rasterExtent)

  def warp(target:RasterExtent) = ArrayRaster(data.warp(rasterExtent,target),target)

  override //for speed
  def foreach(f:Int => Unit):Unit = data.foreach(f)

  def map(f:Int=>Int):Raster = Raster(data.map(f),rasterExtent)
  def combine(r2:Raster)(f:(Int, Int) => Int):Raster = {
    if(this.rasterExtent != r2.rasterExtent) {
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtent does not match ${r2.rasterExtent}")
    }
    r2 match {
      case ar:ArrayRaster => 
        Raster(data.combine(ar.data)(f), rasterExtent)
      case tr:TileRaster =>
        tr.combine(this)((z1,z2)=>f(z2,z1))
    }
  }

  override // for speed
  def foreachDouble(f:Double => Unit):Unit = data.foreachDouble(f)

  def mapDouble(f:Double => Double):Raster = Raster(data.mapDouble(f), rasterExtent)
  def combineDouble(r2:Raster)(f:(Double, Double) => Double):Raster = {
    if(this.rasterExtent != r2.rasterExtent) {
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtent does not match ${r2.rasterExtent}")
    }
    r2 match {
      case ar:ArrayRaster => 
        Raster(data.combineDouble(ar.data)(f), rasterExtent)
      case cr:CroppedRaster =>
        cr.combineDouble(this)((z1,z2) => f(z2,z1))
    }
  }
}
