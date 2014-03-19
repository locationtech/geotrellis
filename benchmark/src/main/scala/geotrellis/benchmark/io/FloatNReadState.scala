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

package geotrellis.benchmark.io

import java.nio.ByteBuffer
import geotrellis._
import geotrellis.data._
import geotrellis.raster._
import geotrellis.util._
import geotrellis.process._
import geotrellis.util.Filesystem

abstract class ArgFloatNReadState(data:Either[String, Array[Byte]],
                                  val rasterExtent:RasterExtent,
                                  val target:RasterExtent,
                                  typ:RasterType) extends ReadState {
  def getType = typ

  final val width:Int = typ.bits / 8

  protected[this] var src:ByteBuffer = null

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }
}

class Float64ReadState(data:Either[String, Array[Byte]],
                       rasterExtent:RasterExtent,
                       target:RasterExtent)
extends ArgFloatNReadState(data, rasterExtent, target, TypeDouble) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, src.getDouble(sourceIndex * width))
  }
}

class Float32ReadState(data:Either[String, Array[Byte]],
                       rasterExtent:RasterExtent,
                       target:RasterExtent)
extends ArgFloatNReadState(data, rasterExtent, target, TypeFloat) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest.updateDouble(destIndex, src.getFloat(sourceIndex * width))
  }
}
