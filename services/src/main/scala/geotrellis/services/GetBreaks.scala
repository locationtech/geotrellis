package geotrellis.service

import geotrellis._
import geotrellis.process._
import geotrellis.source._
import geotrellis.render.ColorRamps._

import Json._

// object LayerService {
//   def getBreaks(layer:LayerId,numBreaks:Int):ValueSource[String] =
//       RasterSource(layer)
//         .classBreaks(numBreaks)
//         .map (Json.classBreaks(_))

//   def getInfo(layer:LayerId):ValueSource[String] =
//     ValueSource(
//       io.LoadRasterLayerInfo(layer)
//         .map { info =>
//           s"""{
//             "name" : "${layer}",
//             "rasterExtent" : ${info.rasterExtent.toJson},
//             "datatype" :" ${info.rasterType}"
//            }"""
//         }
//     )

//   def render(
//     bbox:String,
//     cols:Int,
//     rows:Int,
//     layer:LayerId,
//     breaksString:String,
//     colorRampKey:String
//   ):ValueSource[Array[Byte]] = {
//     val extent = {
//       val Array(xmin,ymin,xmax,ymax) = bbox.split(",").map(_.toDouble)
//       Extent(xmin,ymin,xmax,ymax)
//     }

//     val breaks = breaksString.split(",").map(_.toInt)

//     render(RasterExtent(extent, cols, rows),layer,breaks,colorRampKey)
//   }

//   def render(
//     rasterExtent:RasterExtent,
//     layer:LayerId,
//     breaks:Array[Int],
//     colorRampKey:String
//   ):ValueSource[Array[Byte]] = {
//     val ramp = {
//       val cr = ColorRampMap.getOrElse(colorRampKey,BlueToRed)
//       if(cr.toArray.length < breaks.length) { cr.interpolate(breaks.length) }
//       else { cr }
//     }

//     RasterSource(layer)
//       .renderPng(ramp,breaks)

//     // val layerOp = io.LoadRaster(layer,rasterExtent).map { r =>
//     //   // Convert 0 of bit raster to NODATA
//     //   if(r.rasterType == TypeBit) { 
//     //     r.convert(TypeByte).map { z => if(z == 0) NODATA else z } 
//     //   } else { 
//     //     r 
//     //   }
//     // }                                                 
//   }
// }
