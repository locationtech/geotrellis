package geotrellis.spark.mosaic

import scala.reflect.ClassTag

import org.apache.spark.rdd.{PairRDDFunctions, RDD}

import geotrellis.raster.mosaic.MergeView

class RddMergeMethods[K: ClassTag, TileType: MergeView: ClassTag](
 rdd: RDD[(K, TileType)])
extends MergeMethods[RDD[(K, TileType)]]{
   def merge(other: RDD[(K, TileType)]): RDD[(K, TileType)] = {
     val fMerge = (_: TileType).merge(_: TileType)
     new PairRDDFunctions(rdd)
       .cogroup(other)
       .map { case (key, (myTiles, otherTiles)) =>
         if (myTiles.nonEmpty && otherTiles.nonEmpty) {
           val a = myTiles.reduce(fMerge)
           val b = otherTiles.reduce(fMerge)
           (key, fMerge(a, b))
         } else if (myTiles.nonEmpty) {
           (key, myTiles.reduce(fMerge))
         } else {
           (key, otherTiles.reduce(fMerge))
         }
       }
   }
 }
