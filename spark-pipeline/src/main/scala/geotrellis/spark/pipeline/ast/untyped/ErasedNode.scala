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

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.spark.pipeline.json.TransformTypes._
import geotrellis.spark.pipeline.json._
import geotrellis.spark.pipeline.json.read._
import geotrellis.vector._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.runtime.universe.{TypeTag, typeTag}
import scala.reflect.runtime.universe.Type
import scala.util.Try

/**
  * Erased node type, represents a typed Node with types kept inside.
  * It is a function, as it should operate with nodes constructor types
  * and to "compose" untyped nodes.
  */
trait ErasedNode extends (Any => Any) {
  def maybeApply(x: Any): Option[Node[Any]]

  def apply(x: Any): Any = maybeApply(x) getOrElse {
    throw new Exception(s"Cannot apply ErasedNode to $x " +
      s"since it cannot be cast to $domain")
  }

  def apply(): Any = apply(RealWorld.instance)

  /** Get the typed node without its computation. */
  def node[T: TypeTag]: Node[T] = {
    val thatTpe = typeTag[T].tpe
    if(thatTpe =:= rangeTpe) apply().asInstanceOf[Node[T]]
    else throw new Exception(s"Cannot cast ErasedNode to $thatTpe " +
      s"since it cannot be cast to $rangeTpe")
  }

  /** Compute the result of the node. */
  def unsafeEval(implicit sc: SparkContext): Any = apply().asInstanceOf[Node[Any]].eval

  /** Compute the result of the node and cast to type T. */
  def eval[T: TypeTag](implicit sc: SparkContext): T = {
    val thatTpe = typeTag[T].tpe
    if(thatTpe =:= rangeTpe) unsafeEval.asInstanceOf[T]
    else throw new Exception(s"Cannot cast ErasedNode evaluation result to $thatTpe " +
      s"since it cannot be cast to $rangeTpe")
  }

  def domainTpe: Type

  def rangeTpe: Type

  def domain: String = domainTpe.toString

  def range: String = rangeTpe.toString

  def composable(that: ErasedNode): Boolean = domainTpe =:= that.rangeTpe

  def compose(l: List[ErasedNode]): List[ErasedNodeComposition] =
    l flatMap { compose(_) }

  def compose(inner: ErasedNode): Option[ErasedNodeComposition] = {
    val outer = this
    if (composable(inner)) Some(ErasedNodeComposition(f = outer, g = inner))
    else None
  }

  def unsafeCompose(inner: ErasedNode): ErasedNodeComposition = {
    val outer = this
    if (composable(inner)) ErasedNodeComposition(f = outer, g = inner)
    else throw new Exception(s"Cannot apply ErasedNode to $inner: ${inner.domainTpe} " +
      s"since it cannot be cast to $domain")
  }
}

case class ErasedNodeComposition(f: ErasedNode, g: ErasedNode) extends ErasedNode {
  val domainTpe: Type = g.domainTpe

  val rangeTpe: Type = f.rangeTpe

  def maybeApply(x: Any): Option[Node[Any]] = g.maybeApply(x) flatMap f.maybeApply
}

case class ErasedTypedNode[Domain: TypeTag, Range: TypeTag](constructor: Node[Domain] => Node[Range]) extends ErasedNode {
  def domainTag: TypeTag[Domain] = typeTag[Domain]

  def rangeTag: TypeTag[Range] = typeTag[Range]

  def domainTpe: Type = domainTag.tpe

  def rangeTpe: Type = rangeTag.tpe

  def maybeApply(x: Any): Option[Node[Any]] =
    Try { x.asInstanceOf[Node[Domain]] }
      .toOption
      .map(constructor)
      .map(_.asInstanceOf[Node[Any]])
}

/** Helper functions to convert typed nodes into erased typed nodes */
object ErasedTypedNode {
  def fromRead[Range: TypeTag](node: Input[Range]) =
    ErasedTypedNode[RealWorld, Range](_ => node)

  def fromWrite[Range: TypeTag](constructor: Node[Range] => Output[Range]) =
    ErasedTypedNode[Range, Range](constructor)

  def fromTransform[Domain: TypeTag, Range: TypeTag](constructor: Node[Domain] => ast.Transform[Domain, Range]) =
    ErasedTypedNode[Domain, Range](constructor)
}

/** Class that allows to convert node json representation into an ErasedNode */
case class ErasedJsonNode(arg: PipelineExpr) {
  // TODO: support user defined types
  def toErasedNode: ErasedNode = {
    arg.`type` match {
      case _: SinglebandSpatialExprType => {
        import singleband.spatial._
        arg match {
          case a: JsonRead => {
            arg.`type` match {
              case _: ReadTypes.HadoopReadType => ErasedTypedNode.fromRead(HadoopRead(a))
              case _: ReadTypes.S3ReadType => ErasedTypedNode.fromRead(S3Read(a))
            }
          }
          case a: transform.TileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[RDD[(ProjectedExtent, Tile)]] => TileToLayout(child, a) }
          case a: transform.RetileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[TileLayerRDD[SpatialKey]] => RetileToLayout(child, a) }
          case a: transform.Pyramid =>
            ErasedTypedNode.fromTransform { child: Node[TileLayerRDD[SpatialKey]] => Pyramid(child, a) }
          case a: transform.Reproject => {
            arg.`type` match {
              case _: BufferedReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[TileLayerRDD[SpatialKey]] => BufferedReproject(child, a) }
              case _: PerTileReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[RDD[(ProjectedExtent, Tile)]] => PerTileReproject(child, a) }
            }
          }
          case a: write.JsonWrite =>
            ErasedTypedNode.fromWrite { child: Node[Stream[(Int, TileLayerRDD[SpatialKey])]] => Write(child, a) }
        }
      }
      case _: SinglebandTemporalExprType => {
        import singleband.temporal._
        arg match {
          case a: JsonRead => {
            arg.`type` match {
              case _: ReadTypes.HadoopReadType => ErasedTypedNode.fromRead(HadoopRead(a))
              case _: ReadTypes.S3ReadType => ErasedTypedNode.fromRead(S3Read(a))
            }
          }
          case a: transform.TileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[RDD[(TemporalProjectedExtent, Tile)]] => TileToLayout(child, a) }
          case a: transform.RetileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[TileLayerRDD[SpaceTimeKey]] => RetileToLayout(child, a) }
          case a: transform.Pyramid =>
            ErasedTypedNode.fromTransform { child: Node[TileLayerRDD[SpaceTimeKey]] => Pyramid(child, a) }
          case a: transform.Reproject => {
            arg.`type` match {
              case _: BufferedReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[TileLayerRDD[SpaceTimeKey]] => BufferedReproject(child, a) }
              case _: PerTileReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[RDD[(TemporalProjectedExtent, Tile)]] => PerTileReproject(child, a) }
            }
          }
          case a: write.JsonWrite =>
            ErasedTypedNode.fromWrite { child: Node[Stream[(Int, TileLayerRDD[SpaceTimeKey])]] => Write(child, a) }
        }
      }
      case _: MultibandSpatialExprType => {
        import multiband.spatial._
        arg match {
          case a: JsonRead => {
            arg.`type` match {
              case _: ReadTypes.HadoopReadType => ErasedTypedNode.fromRead(HadoopRead(a))
              case _: ReadTypes.S3ReadType => ErasedTypedNode.fromRead(S3Read(a))
            }
          }
          case a: transform.TileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[RDD[(ProjectedExtent, MultibandTile)]] => TileToLayout(child, a) }
          case a: transform.RetileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[MultibandTileLayerRDD[SpatialKey]] => RetileToLayout(child, a) }
          case a: transform.Pyramid =>
            ErasedTypedNode.fromTransform { child: Node[MultibandTileLayerRDD[SpatialKey]] => Pyramid(child, a) }
          case a: transform.Reproject => {
            arg.`type` match {
              case _: BufferedReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[MultibandTileLayerRDD[SpatialKey]] => BufferedReproject(child, a) }
              case _: PerTileReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[RDD[(ProjectedExtent, MultibandTile)]] => PerTileReproject(child, a) }
            }
          }
          case a: write.JsonWrite =>
            ErasedTypedNode.fromWrite { child: Node[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]] => Write(child, a) }
        }
      }
      case _: MultibandTemporalExprType => {
        import multiband.temporal._
        arg match {
          case a: JsonRead => {
            arg.`type` match {
              case _: ReadTypes.HadoopReadType => ErasedTypedNode.fromRead(HadoopRead(a))
              case _: ReadTypes.S3ReadType => ErasedTypedNode.fromRead(S3Read(a))
            }
          }
          case a: transform.TileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[RDD[(TemporalProjectedExtent, MultibandTile)]] => TileToLayout(child, a) }
          case a: transform.RetileToLayout =>
            ErasedTypedNode.fromTransform { child: Node[MultibandTileLayerRDD[SpaceTimeKey]] => RetileToLayout(child, a) }
          case a: transform.Pyramid =>
            ErasedTypedNode.fromTransform { child: Node[MultibandTileLayerRDD[SpaceTimeKey]] => Pyramid(child, a) }
          case a: transform.Reproject => {
            arg.`type` match {
              case _: BufferedReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[MultibandTileLayerRDD[SpaceTimeKey]] => BufferedReproject(child, a) }
              case _: PerTileReprojectType =>
                ErasedTypedNode.fromTransform { child: Node[RDD[(TemporalProjectedExtent, MultibandTile)]] => PerTileReproject(child, a) }
            }
          }
          case a: write.JsonWrite =>
            ErasedTypedNode.fromWrite { child: Node[Stream[(Int, MultibandTileLayerRDD[SpaceTimeKey])]] => Write(child, a) }
        }
      }
    }
  }
}

object ErasedUtils {
  def compose(l: List[ErasedNode], depth: Int) =
    0.until(depth).foldLeft(l) { case (list, _) =>
      list ++ list.flatMap { function => function compose list filter { _ != function } }
    }

  /** This step in fact TypeChecks pipeline expr tree and checks how it can be composed */
  def fromPipelineExprList(l: List[PipelineExpr]): ErasedNode =
    ErasedUtils.buildComposition(l.map(ErasedJsonNode(_).toErasedNode).reverse)

  /** Final function, which can be just applied to a some type */
  def buildComposition(l: List[ErasedNode]): ErasedNode =
    l.reduceLeft[ErasedNode] { case (fst, snd) => fst.unsafeCompose(snd) }

  def eprint(ef: ErasedNode): Unit =
    println(ef.domainTpe.toString + " => " + ef.rangeTpe.toString)

  def cprint(ef: ErasedNodeComposition, depth: Int): Unit = {
    eprint(ef.f, depth)
    eprint(ef.g, depth)
  }

  def eprint(ef: ErasedNode, depth: Int): Unit = {
    ef match {
      case ErasedNodeComposition(f, g) => eprint(f, depth + 1); eprint(g, depth + 1)
      case f: ErasedNode => eprint(f)
    }
  }
}
