package geotrellis.spark.pipeline.ast.untyped

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.transform
import geotrellis.spark.pipeline.json.TransformTypes._
import geotrellis.spark.pipeline.json.WriteTypes._
import geotrellis.spark.pipeline.json._
import geotrellis.spark.pipeline.json.read._
import geotrellis.vector._

import org.apache.spark.rdd.RDD

import scala.reflect.runtime.universe.{TypeTag, typeTag}
import scala.reflect.runtime.universe.Type
import scala.util.Try

trait ErasedNode extends (Any => Any) {
  def maybeApply(x: Any): Option[Any]

  def apply(x: Any): Any = maybeApply(x) getOrElse {
    throw new Exception(s"Cannot apply ErasedNode to $x " +
      s"since it cannot be cast to $domain")
  }

  def apply(): Any = apply(null)

  def get[T <: Node[_]: TypeTag]: T = {
    val thatTpe = typeTag[T].tpe
    if(thatTpe.toString.contains(rangeTpe.toString)) apply().asInstanceOf[T]
    else throw new Exception(s"Cannot cast ErasedNode to $thatTpe " +
      s"since it cannot be cast to $rangeTpe")
  }

  def domainTpe: Type

  def rangeTpe: Type

  def domain = domainTpe.toString

  def range = rangeTpe.toString

  def composable(that: ErasedNode) = domainTpe == that.rangeTpe

  def compose(l: List[ErasedNode]): List[ErasedNodeComposition] =
    l flatMap { compose(_) }

  def compose(inner: ErasedNode): Option[ErasedNodeComposition] = {
    val outer = this
    if (composable(inner)) Some(ErasedNodeComposition(f = outer, g = inner))
    else None
  }

  def composeUnsafe(inner: ErasedNode): ErasedNodeComposition = {
    val outer = this

    if (composable(inner)) ErasedNodeComposition(f = outer, g = inner)
    else throw new Exception(s"Cannot apply ErasedNode to $inner: ${inner.domainTpe} " +
      s"since it cannot be cast to $domain")
  }
}

case class ErasedNodeComposition(f: ErasedNode, g: ErasedNode) extends ErasedNode {
  val domainTpe = g.domainTpe

  val rangeTpe = f.rangeTpe

  def maybeApply(x: Any) = g.maybeApply(x) flatMap f.maybeApply
}

case class ErasedTypedNode[Domain: TypeTag, Range: TypeTag](constructor: Node[Domain] => Node[Range]) extends ErasedNode {
  def domainTag = typeTag[Domain]

  def rangeTag = typeTag[Range]

  def domainTpe = domainTag.tpe

  def rangeTpe = rangeTag.tpe

  // TODO: replace with shapeless Typeable
  def maybeApply(x: Any): Option[Node[Any]] =
    Try { x.asInstanceOf[Node[Domain]] }
      .toOption
      .map(constructor)
      .map(_.asInstanceOf[Node[Any]])
}

object ErasedTypedNode {
  def fromRead[Range: TypeTag](node: geotrellis.spark.pipeline.ast.Read[Range]) =
    ErasedTypedNode[Any, Range](_ => node)

  def fromWrite[Range: TypeTag](constructor: Node[Range] => geotrellis.spark.pipeline.ast.Write[Range]) =
    ErasedTypedNode[Range, Range](constructor)

  def fromTransform[Domain: TypeTag, Range: TypeTag](constructor: Node[Domain] => geotrellis.spark.pipeline.ast.Transform[Domain, Range]) =
    ErasedTypedNode[Domain, Range](constructor)
}

// TODO: support user defined types
case class ErasedJsonNode(arg: PipelineExpr) {
  def toErasedNode: ErasedNode = {
    arg.`type` match {
      case _: SinglebandSpatialExprType => {
        import singleband.spatial._
        arg match {
          case a: JsonRead =>
            ErasedTypedNode.fromRead(HadoopRead(a))
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
          case a: write.JsonWrite => {
            arg.`type` match {
              case _: HadoopType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, TileLayerRDD[SpatialKey])]] => HadoopWrite(child, a) }
              case _: FileType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, TileLayerRDD[SpatialKey])]] => FileWrite(child, a) }
            }
          }
        }
      }
      case _: SinglebandTemporalExprType => {
        import singleband.temporal._
        arg match {
          case a: JsonRead =>
            ErasedTypedNode.fromRead(HadoopRead(a))
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
          case a: write.JsonWrite => {
            arg.`type` match {
              case _: HadoopType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, TileLayerRDD[SpaceTimeKey])]] => HadoopWrite(child, a) }
              case _: FileType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, TileLayerRDD[SpaceTimeKey])]] => FileWrite(child, a) }
            }
          }
        }
      }
      case _: MultibandSpatialExprType => {
        import multiband.spatial._
        arg match {
          case a: JsonRead =>
            ErasedTypedNode.fromRead(HadoopRead(a))
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
          case a: write.JsonWrite => {
            arg.`type` match {
              case _: HadoopType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]] => HadoopWrite(child, a) }
              case _: FileType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, MultibandTileLayerRDD[SpatialKey])]] => FileWrite(child, a) }
            }
          }
        }
      }
      case _: MultibandTemporalExprType => {
        import multiband.temporal._
        arg match {
          case a: JsonRead =>
            ErasedTypedNode.fromRead(HadoopRead(a))
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
          case a: write.JsonWrite => {
            arg.`type` match {
              case _: HadoopType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, MultibandTileLayerRDD[SpaceTimeKey])]] => HadoopWrite(child, a) }
              case _: FileType =>
                ErasedTypedNode.fromWrite { child: Node[Stream[(Int, MultibandTileLayerRDD[SpaceTimeKey])]] => FileWrite(child, a) }
            }
          }
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
  def buildComposition(l: List[ErasedNode]): ErasedNode = l.reduceLeft[ErasedNode] { case (fst, snd) => fst.composeUnsafe(snd) }

  def eprint(ef: ErasedNode) =
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
