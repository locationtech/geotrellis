/*
 * Copyright (c) 2014 DigitalGlobe.
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
import geotrellis.vector._
import geotrellis.proj4._

import geotrellis.spark.tiling._
import geotrellis.spark.rdd._

import org.apache.spark.rdd._

import spire.syntax.cfor._

package object spark {
  type TileId = Long
  type ProjectedExtent = (Extent, CRS)
  type Dimensions = (Int, Int)
  type TileBounds = GridBounds

  /** The thing I miss the most from F# */
  implicit class toPipe[A](x : A) { 
    def |> [T](f : A => T) = f(x) 
  }

  implicit class toPipe2[A, B](tup : (A, B)) {
    def |> [T](f : (A, B) => T) = f(tup._1, tup._2) 
  }

  implicit class toPipe3[A, B, C](tup : (A, B, C)) {
    def |> [T](f : (A, B, C) => T) = f(tup._1, tup._2, tup._3) 
  }

  implicit class toPipe4[A, B, C, D](tup : (A, B, C, D)) {
    def |> [T](f : (A, B, C, D) => T) = f(tup._1, tup._2, tup._3, tup._4)
  }

  implicit def tmsTileRddToTupleRdd(rdd: RDD[TmsTile]): RDD[(Long, Tile)] =
    rdd.map { case TmsTile(id, tile) => (id, tile) }

  implicit def tupleRddToTmsTileRdd(rdd: RDD[(Long, Tile)]): RDD[TmsTile] =
    rdd.map { case (id, tile) => TmsTile(id, tile) }

  implicit def tmsTileRddToPairRddFunctions(rdd: RDD[TmsTile]): PairRDDFunctions[Long, Tile] =
    new PairRDDFunctions(tmsTileRddToTupleRdd(rdd))

  implicit def tmsTileRddToOrderedRddFunctions(rdd: RDD[TmsTile]): OrderedRDDFunctions[Long, Tile, (Long, Tile)] =
    new OrderedRDDFunctions(tmsTileRddToTupleRdd(rdd))
}
