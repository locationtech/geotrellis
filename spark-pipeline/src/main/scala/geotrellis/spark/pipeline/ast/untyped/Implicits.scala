/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.pipeline.ast.untyped

import geotrellis.spark.pipeline.ast.Node
import geotrellis.spark.pipeline.json._

import cats.implicits._
import io.circe
import io.circe.parser.decode
import org.apache.spark.SparkContext

import scala.reflect.runtime.universe.TypeTag

object Implicits extends Implicits

trait Implicits {
  implicit class withPipelineExpressionExtensions(list: List[PipelineExpr]) {
    def erasedNode: ErasedNode = ErasedUtils.fromPipelineExprList(list)
    def node[T: TypeTag]: Node[T] = erasedNode.node[T]
    def unsafeEval(implicit sc: SparkContext): Any = erasedNode.unsafeEval
    def eval[T: TypeTag](implicit sc: SparkContext): T = erasedNode.eval[T]
  }

  implicit class withStringExtensions(json: String) {
    def pipelineExpr: Either[circe.Error, List[PipelineExpr]] = decode[List[PipelineExpr]](json)
    def erasedNode: Option[ErasedNode] = pipelineExpr.map(_.erasedNode).toOption
    def node[T: TypeTag]: Option[Node[T]] = erasedNode.map(_.node[T])
    def unsafeEval(implicit sc: SparkContext): Option[Any] = erasedNode.map(_.unsafeEval)
    def eval[T: TypeTag](implicit sc: SparkContext): Option[T] = erasedNode.map(_.eval[T])
  }
}
