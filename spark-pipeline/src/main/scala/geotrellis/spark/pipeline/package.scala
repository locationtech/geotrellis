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

package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.spark.pipeline.ast.Node
import geotrellis.spark.pipeline.json.PipelineExpr

import _root_.io.circe._
import _root_.io.circe.syntax._
import cats.Monoid

import scala.util.Try

package object pipeline extends json.Implicits with ast.untyped.Implicits {
  type PipelineConstructor = List[PipelineExpr]

  implicit class withPipelineConstructor(list: PipelineConstructor) {
    def ~(e: PipelineExpr): PipelineConstructor = list :+ e
    def ~(e: Option[PipelineExpr]): PipelineConstructor = e.fold(list)(el => list :+ el)
    def map[B](f: PipelineExpr => B): List[B] = list.map(f)
  }

  implicit class withPipelinePrettyPrint(that: Node[_]) {
    def prettyPrint: String = that.asJson.asJson.pretty(pipelineJsonPrinter)
  }

  implicit class withGetCRS[T <: { def crs: String }](o: T) {
    def getCRS = Try(CRS.fromName(o.crs)) getOrElse CRS.fromString(o.crs)
  }

  implicit class withGetOptionCRS[T <: { def crs: Option[String] }](o: T) {
    def getCRS = o.crs.map(c => Try(CRS.fromName(c)) getOrElse CRS.fromString(c))
  }

  implicit val jsonDeepMergeMonoid: Monoid[Json] = new Monoid[Json] {
    val empty = Json.Null
    def combine(x: Json, y: Json): Json = x.deepMerge(y)
  }
}
