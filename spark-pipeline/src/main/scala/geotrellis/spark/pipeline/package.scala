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

      (map.getOrElse("read", Nil), map.getOrElse("transform", Nil), map.getOrElse("write", Nil))
    }

    def eval[I, K, V, M[_]] = {
      val reads: Map[String, RDD[(I, V)]] =
        read.map { case r: Read => r.getTag -> r.eval[I, V] }.toMap

      // first group operations
      val group = transform.collect { case t: TransformGroup => t }
      val merge = transform.collect { case t: TransformMerge => t }

      group.map { t =>
        val r = t.tags.flatMap(reads.get(_))
      }

    }
  }
}
