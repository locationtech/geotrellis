package geotrellis.spark

import org.apache.spark.rdd.RDD

package object pipeline {
  type PipelineConstructor = List[PipelineExpr]

  implicit class PipelineMethods(pipeline: List[PipelineExpr]) {
    lazy val (read, transform, write) = {
      val map =
        pipeline
          .zipWithIndex
          .groupBy(_._1.`type`.split("\\.").head)
          .map { case (n, l) => n -> l.sortBy(_._2).map(_._1) }

      (
        map.getOrElse("read", Nil).collect { case r: Read => r },
        map.getOrElse("transform", Nil).collect { case t: Transform => t },
        map.getOrElse("write", Nil).collect { case w: Write => w }
      )
    }

    def interpretTransform[I, K, V, M[_]](t: Transform) = t match {
      case tr: TransformPerTileReproject => tr.eval _
    }

    /**
      * The problem: different eval signatures of different operations
      * Solutions?
      *
      * Type erase everything and to cast from Any?
      *
      * */

    def execute[I, K, V, M[_]] = {
      val reads: Map[String, RDD[(I, V)]] =
        read.map { r => r.getTag -> r.eval[I, V] }.toMap

      // first group operations
      val group = transform.collect { case t: TransformGroup => t }
      //val merge = transform.collect { case t: TransformMerge => t }

      val groupApply: List[(String, RDD[(I, V)])] = group.flatMap { _.eval(reads.toList) }

      groupApply.foldLeft(List[RDD[(K, V)] with M[K]]()) { case (acc, lrdd: (String, RDD[(I, V)])) =>
        acc
      }



      // (I, V) => (I, V) ops
      var isBuffered = true
      val reprojectedPerTile: List[(String, RDD[(I, V)])] = transform.flatMap {
        _ match {
          case t: TransformPerTileReproject => {
            isBuffered = false
            groupApply.map { case (k, v) => k -> t.eval[I, V](v) }
          }

          case _ => groupApply
        }
      }

      // here apply some user functions

      val tileToLayout: Seq[RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] = transform.flatMap {
        _ match {
          case t: TransformTile => {
            reprojectedPerTile.map { case (_, rdd) =>
              t.eval[K, I, V](rdd)
            }
          }
        }
      }

      // here apply some user functions


      val reprojectedBuffered: Seq[(Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]])] = transform.flatMap {
        _ match {
          case t: TransformBufferedReproject => {
            tileToLayout.map { case rdd =>
              t.eval[K, V](rdd)(null)
            }
          }
        }
      }

    }
  }
}
