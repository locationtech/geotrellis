/***
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
 ***/

package demo

import javax.servlet.http.{HttpServletRequest}
import javax.ws.rs.{GET, Path, DefaultValue, PathParam, QueryParam}
import javax.ws.rs.core.{Response, Context}

import geotrellis._
import geotrellis.source._
import geotrellis.render._
import ColorRamps._

@Path("/greeting")
class HelloWorld {
  @GET
  def get(@Context req:HttpServletRequest) = {
    // give a friendly greeting
    response("text/plain")("hello world")
  }
}

@Path("/bbox")
class BoundingBox {
  @GET
  @Path("/{extent1}/union/{extent2}")
  def get(@PathParam("extent1") s1:String,
          @PathParam("extent2") s2:String,
          @Context req:HttpServletRequest) = {
    // parse the given extents
    val extent1 = {
      val Array(xmin,ymin,xmax,ymax) = s1.split(",").map(_.toDouble)
      Extent(xmin,ymin,xmax,ymax)
    }

    val extent2 = {
      val Array(xmin,ymin,xmax,ymax) = s2.split(",").map(_.toDouble)
      Extent(xmin,ymin,xmax,ymax)
    }

    val combined = extent1.combine(extent2)

    response("text/plain")(combined.toString)
  }
}

@Path("/simpleDraw")
class SimpleDrawRaster {
  @GET
  @Path("/{name}")
  def get(@PathParam("name") name:String) = {
    val raster:RasterSource = RasterSource(name)
    val png:ValueSource[Png] = raster.renderPng(BlueToRed)

    // run the source
    try {
      val img:Array[Byte] = png.get
      response("image/png")(img)
    } catch {
      case e:Throwable => response("text/plain")(e.toString)
    }
  }
}

@Path("/draw")
class DrawRaster {
  @GET
  @Path("/{name}/palette/{palette}/shades/{shades}")
  def get(@PathParam("name") name:String,
          @PathParam("palette") palette:String,
          @PathParam("shades") shades:Int,
          @Context req:HttpServletRequest) = {

    // load the raster
    val raster:RasterSource = RasterSource(name)

    // find the colors to use
    val paletteColors:Array[Int] = palette.split(",").map(Color.parseColor(_))
    val colors:Array[Int] = Color.chooseColors(paletteColors, shades)

    val png = raster.renderPng(colors)

    // run the operation
    try {
      val img:Array[Byte] = png.get
      response("image/png")(img)
    } catch {
      case e:Throwable => response("text/plain")(e.toString)
    }
  }
}
