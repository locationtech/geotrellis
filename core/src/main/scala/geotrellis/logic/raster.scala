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

package geotrellis.logic

import geotrellis._
//import geotrellis.raster.{TileLayout,TileArrayRasterData}
import scala.annotation.tailrec

// abstract class TiledRasterMapper(r:Op[Raster]) extends Operation[Raster] {
//   def mapper(rOp:Op[Raster]):Op[Raster]

//   private case class TileInfo(tileLayout:TileLayout, rasterExtent: RasterExtent)
//   var limit = 1000

//   def _run() = {
//     if (r.isInstanceOf[TiledRasterMapper]) {
//       AndThen(mapper(r))
//     } else {
//       runAsync('init :: r :: Nil)
//     }
//   }

//   val nextSteps:Steps = {
//     case 'init :: (r:Raster) :: Nil => init(r)
//     case 'runGroup :: (tileInfo:TileInfo) :: (oldResults:List[_]) :: (pending:List[_]) :: (_newResults: List[_]) => {
//       val newResults = _newResults.asInstanceOf[List[Raster]]
//       val results = oldResults.asInstanceOf[List[Raster]] ::: newResults
//       pending match {
//         case Nil => Result(reducer(tileInfo, results)) 
//         case (head: List[_]) :: tail => {
//           runAsync('runGroup :: tileInfo :: results :: tail :: head)
//         }
//         case _ => throw new Exception("unexpected state")
//       }
//     }
//   }

//   def reducer(tileInfo:TileInfo, rasters:List[Raster]) = {
//     Raster(TileArrayRasterData(rasters.toArray, tileInfo.tileLayout), tileInfo.rasterExtent)
//   }

//   def init(r:Raster) = {
//     if (r.isTiled) {
//       val tileLayout = r.data.tileLayoutOpt.get
//       val tileInfo = TileInfo(tileLayout, r.rasterExtent)
//       val ops = r.getTileOpList().map(mapper).map(raster.op.Force(_))
//       val groups = ops.grouped(limit).toList
//       val tail = groups.tail
//       val head = groups.head
//       runAsync('runGroup :: tileInfo :: List[Raster]() :: tail :: head)
//     } else {
//       AndThen(mapper(r))
//     }
//   }
// }

// case class RasterMapIfSet(r:Op[Raster])(f:Int => Int) extends TiledRasterMapper(r) {
//   def mapper(rOp:Op[Raster]) = rOp.map(_.mapIfSet(f))
// }

// case class RasterMap(r:Op[Raster])(f:Int => Int) extends TiledRasterMapper(r) {
//   def mapper(rOp:Op[Raster]):Op[Raster] = rOp.map(_.map(f))
// }

// case class RasterDualMap(r:Op[Raster])(f:Int => Int)(g:Double => Double) extends TiledRasterMapper(r) {
//   def mapper(rOp:Op[Raster]):Op[Raster] = rOp.map(_.dualMap(f)(g))
// }

// case class RasterDualMapIfSet(r:Op[Raster])(f:Int => Int)(g:Double => Double) extends TiledRasterMapper(r) {
//   def mapper(rOp:Op[Raster]):Op[Raster] = rOp.map(_.dualMapIfSet(f)(g))
// }

// case class RasterCombine(r1:Op[Raster], r2:Op[Raster])(f:(Int,Int) => Int) extends Op2(r1, r2)({
//   (r1, r2) => Result(r1.combine(r2)(f).force)
// })

// case class RasterDualCombine(r1:Op[Raster], r2:Op[Raster])(f:(Int,Int) => Int)(g:(Double,Double) => Double) extends Op2(r1,r2)({
//   (r1, r2) => Result(r1.dualCombine(r2)(f)(g).force)
// })

// case class RasterForce(rOp:Op[Raster]) extends TiledRasterMapper(rOp) {
//   def mapper(rOp:Op[Raster]) = rOp.map { _.force }
// }
 
// case class RasterDualReduce(rasters:Seq[Op[Raster]])(f:(Int,Int) => Int)(g:(Double,Double) => Double) extends Operation[Raster] {
//   def _run() = runAsync(rasters.toList)

//   val nextSteps:Steps = {
//     case rasters:List[_] => handleRasters(rasters.asInstanceOf[List[Raster]])
//   }

//   @tailrec final def reduce(d:RasterData, rasters:List[Raster]):RasterData = {
//     rasters match {
//       case Nil => d
//       case r :: rs => if (r.isFloat) {
//         reduceDouble(d.combineDouble(r.data)(g), rs)
//       } else {
//         reduce(d.combine(r.data)(f), rs)
//       }
//     }
//   }

//   @tailrec final def reduceDouble(d:RasterData, rasters:List[Raster]):RasterData = {
//     rasters match {
//       case Nil => d
//       case r :: rs => reduceDouble(d.combineDouble(r.data)(g), rs)
//     }
//   }

//   def handleRasters(rasters:List[Raster]) = {
//     val (r :: rs) = rasters
//     if (r.isFloat) {
//       AndThen(raster.op.Force(Raster(reduceDouble(r.data, rs), r.rasterExtent)))
//     } else {
//       AndThen(raster.op.Force(Raster(reduce(r.data, rs), r.rasterExtent)))
//     }
//   }
// }
