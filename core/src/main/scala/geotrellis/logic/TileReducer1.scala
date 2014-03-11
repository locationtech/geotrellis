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

import scala.math.{ min, max }

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.raster._
import geotrellis.feature.Polygon

/**
 * Base class for operations that use a map and reduce step to allow for
 * operations to be distributed over [[geotrellis.raster.TiledRasterData]].
 */
// trait TileReducer1[C] extends Op[C] {
//   type B
  
//   val r: Op[Raster]
//   var limit: Int = 30

//   def loadTileExtent:Option[Op[Polygon[_]]] = None

//   def mapper(r: Op[Raster]): Op[List[B]]
//   def reducer(mapResults: List[B]): C

//   def _run() = {
//     loadTileExtent match {
//       case None => runAsync('init :: r :: Nil)
//       case Some(op) => runAsync('initWithTileExtent :: r :: op :: Nil)
//     }
//   }

//   val nextSteps: Steps = {
//     case 'init :: (r: Raster) :: Nil => init(r, None)
//     case 'initWithTileExtent :: (r: Raster) :: (p:Polygon[_]) :: Nil => init(r,Some(p))
//     case 'reduce :: (bs: List[_]) => {
//       val results = bs.asInstanceOf[List[List[B]]].flatten
//       Result(reducer(results))
//     }
//     case 'runGroup :: (oldResults: List[_]) :: (bs: List[_]) :: (newResults: List[_]) => {
//       val newResults2 = newResults.asInstanceOf[List[List[B]]]
//       val results = oldResults.asInstanceOf[List[B]] ::: newResults2.flatten
//       bs match {
//         case Nil => Result(reducer(results))
//         case (head: List[_]) :: tail => {
//           runAsync('runGroup :: results :: tail :: head)
//         }
//         case _ => throw new Exception("unexpected state in tile reducer")
//       }
//     }
//   }

//   def init(r: Raster, cropPolygon:Option[Polygon[_]]) = {
//     if (r.isTiled) {
//       val ops = cropPolygon match {
//         case None => r.getTileOpList().map(mapper)
//         case Some(p) => r.getTileOpList(p).map(mapper)
//       }
//       if (ops.isEmpty) { 
//         Result(reducer(Nil))
//       } else {
//         val groups = ops.grouped(limit).toList
//         val tail = groups.tail
//         val head = groups.head
//         runAsync('runGroup :: List[B]() :: tail :: head)
//       } 
//    } else {
//       runAsync('reduce :: mapper(r) :: Nil)
//    }
//   }
// }
