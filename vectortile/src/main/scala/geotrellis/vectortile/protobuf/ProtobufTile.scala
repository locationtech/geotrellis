/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.vectortile.protobuf

import geotrellis.vector._
import geotrellis.vectortile.{Layer, VectorTile}
import geotrellis.vectortile.protobuf.internal._
import geotrellis.vectortile.protobuf.internal.{vector_tile => vt}
import geotrellis.vectortile.protobuf.internal.vector_tile.Tile.GeomType.{POINT, LINESTRING, POLYGON}

import scala.collection.mutable.ListBuffer

// --- //

/**
  * A concrete representation of a VectorTile, as one decoded from Protobuf
  * bytes. This is the original/default type for VectorTiles.
  *
  * {{{
  * import geotrellis.vectortile.protobuf._
  *
  * val bytes: Array[Byte] = ...  // from some `.mvt` file
  * val key: SpatialKey = ...  // preknown
  * val layout: LayoutDefinition = ...  // preknown
  * val tileExtent: Extent = layout.mapTransform(key)
  *
  * val tile: VectorTile = ProtobufTile.fromBytes(bytes, tileExtent)
  * }}}
  *
  * @constructor This is not meant to be called directly. See this class's
  * companion object for the available helper methods.
  */
case class ProtobufTile(
  layers: Map[String, ProtobufLayer],
  tileExtent: Extent
) extends VectorTile {
  /** Encode this VectorTile back into a mid-level Protobuf object. */
  def toProtobuf: vt.Tile =
    vt.Tile(layers = layers.values.map(_.toProtobuf).toSeq)

  /** Encode this VectorTile back into its original form of Protobuf bytes. */
  def toBytes: Array[Byte] = toProtobuf.toByteArray
}

object ProtobufTile {
  /** Create a ProtobufTile masked as its parent trait. */
  def fromPBTile(
    tile: vt.Tile,
    tileExtent: Extent
  ): VectorTile = {

    val layers: Map[String, ProtobufLayer] = tile.layers.map({ l =>
      val pbl = ProtobufLayer(l, tileExtent)

      pbl.name -> pbl
    }).toMap

    new ProtobufTile(layers, tileExtent)
  }

  /** Create a [[VectorTile]] from raw Protobuf bytes.
    *
    * @param bytes  Raw Protobuf bytes from a `.mvt` file or otherwise.
    * @param tileExtent The [[Extent]] of this tile, '''not''' the global extent.
    */
  def fromBytes(
    bytes: Array[Byte],
    tileExtent: Extent
  ): VectorTile = {
    fromPBTile(vt.Tile.parseFrom(bytes), tileExtent)
  }
}

/**
  * A [[Layer]] decoded from Protobuf data. All of its Features are decoded
  * lazily, making for very fast extraction of single features/geometries.
  *
  */
case class ProtobufLayer(
  private val rawLayer: vt.Tile.Layer,
  private val tileExtent: Extent
) extends Layer {
  /* Expected fields */
  def name: String = rawLayer.name
  def tileWidth: Int = rawLayer.extent.getOrElse(4096)

  /** The version of the specification that this Layer adheres to. */
  def version: Int = rawLayer.version

  def features: Seq[Feature[Geometry, Map[String, Value]]] = {
    points.append(multiPoints)
     .append(lines)
     .append(multiLines)
     .append(polygons)
     .append(multiPolygons)
  }

  /* Unconsumed raw Features */
  // TODO Should this be a def?
  private val (pointFs, lineFs, polyFs) = segregate(rawLayer.features)

  /** How much of the [[Extent]] is covered by a single grid coordinate? */
  private def resolution: Double = tileExtent.height / tileWidth

  /**
   * Polymorphically generate a [[Stream]] of parsed Geometries and
   * their metadata.
   */
  private def geomStream[G1 <: Geometry, G2 <: MultiGeometry](
    feats: ListBuffer[vt.Tile.Feature]
  )(implicit protobufGeom: ProtobufGeom[G1, G2]): Stream[(Either[G1, G2], Map[String, Value])] = {
    def loop(fs: ListBuffer[vt.Tile.Feature]): Stream[(Either[G1, G2], Map[String, Value])] = {
      if (fs.isEmpty) {
        Stream.empty[(Either[G1, G2], Map[String, Value])]
      } else {
        val geoms = fs.head.geometry
        val g = protobufGeom.fromCommands(Command.commands(geoms), tileExtent.northWest, resolution)

        (g, getMeta(rawLayer.keys, rawLayer.values, fs.head.tags)) #:: loop(fs.tail)
      }
    }

    loop(feats)
  }

  /**
   * Construct Feature-specific metadata from the key/value lists of
   * the parent layer.
   */
  private def getMeta(keys: Seq[String], vals: Seq[vt.Tile.Value], tags: Seq[Int]): Map[String, Value] = {
    val pairs = new ListBuffer[(String, Value)]
    var i = 0

    while (i < tags.length) {
      val k: String = keys(tags(i))
      val v: vt.Tile.Value = vals(tags(i + 1))

      pairs.append(k -> v)

      i += 2
    }

    pairs.toMap
  }

  /* Geometry Streams */
  private lazy val pointStream = geomStream[Point, MultiPoint](pointFs)
  private lazy val lineStream = geomStream[Line, MultiLine](lineFs)
  private lazy val polyStream = geomStream[Polygon, MultiPolygon](polyFs)

  /* OPTIMIZATION NOTES
   * `Stream.flatMap` maintains laziness. A common pattern here to "fold away"
   * results you don't want is to use [[Option]]. However, flatMap here
   * expects an [[Iterable]], and employs an implicit conversion from [[Option]]
   * to get it.
   *
   * By calling directly what that implicit eventually calls at the bottom of
   * its call stack, we save some operations.
   */
  lazy val points: Stream[Feature[Point, Map[String, Value]]] = pointStream
    .flatMap({
      case (Left(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val multiPoints: Stream[Feature[MultiPoint, Map[String, Value]]] = pointStream
    .flatMap({
      case (Right(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val lines: Stream[Feature[Line, Map[String, Value]]] = lineStream
    .flatMap({
      case (Left(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val multiLines: Stream[Feature[MultiLine, Map[String, Value]]] = lineStream
    .flatMap({
      case (Right(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val polygons: Stream[Feature[Polygon, Map[String, Value]]] = polyStream
    .flatMap({
      case (Left(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val multiPolygons: Stream[Feature[MultiPolygon, Map[String, Value]]] = polyStream
    .flatMap({
      case (Right(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  /**
   * Given a raw protobuf Layer, segregate its Features by their GeomType.
   * `UNKNOWN` geometry types are ignored.
   */
  private def segregate(
    features: Seq[vt.Tile.Feature]
  ): (ListBuffer[vt.Tile.Feature], ListBuffer[vt.Tile.Feature], ListBuffer[vt.Tile.Feature]) = {
    val points = new ListBuffer[vt.Tile.Feature]
    val lines = new ListBuffer[vt.Tile.Feature]
    val polys = new ListBuffer[vt.Tile.Feature]

    features.foreach { f =>
      f.getType match {
        case POINT => points.append(f)
        case LINESTRING => lines.append(f)
        case POLYGON => polys.append(f)
        case _ => Unit // `UNKNOWN` or `Unrecognized`.
      }
    }

    (points, lines, polys)
  }

  /** Encode this Layer back into a mid-level Protobuf object. */
  def toProtobuf: vt.Tile.Layer = {
    val pgp = implicitly[ProtobufGeom[Point, MultiPoint]]
    val pgl = implicitly[ProtobufGeom[Line, MultiLine]]
    val pgy = implicitly[ProtobufGeom[Polygon, MultiPolygon]]

    val (keys, values) = totalMeta

    /* In a future version of the VectorTile spec, when Single and Multi
     * Geometries are separate, we will be able to restructre `ProtobufGeom`
     * in such a way that makes `Geometry.toCommands` possible here.
     *
     * `unfeature` will become polymorphic, so calls to it will look like:
     *
     *   points.map(f => unfeature(keys, values, f))
     */
    val features = Seq(
      points.map(f => unfeature(keys, values, POINT, pgp.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiPoints.map(f => unfeature(keys, values, POINT, pgp.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data)),
      lines.map(f => unfeature(keys, values, LINESTRING, pgl.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiLines.map(f => unfeature(keys, values, LINESTRING, pgl.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data)),
      polygons.map(f => unfeature(keys, values, POLYGON, pgy.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiPolygons.map(f => unfeature(keys, values, POLYGON, pgy.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data))
    ).flatten

    vt.Tile.Layer(version, name, features, keys, values.map(_.toProtobuf), Some(tileWidth))
  }

  private def totalMeta: (Seq[String], Seq[Value]) = {
    /* Pull into memory once to avoid GC on the feature list */
    val fs: Seq[Feature[Geometry, Map[String, Value]]] = features

    /* Must be unique */
    val keys: Seq[String] = fs.map(_.data.keys).flatten.distinct

    val values: Seq[Value] = fs.map(_.data.values).flatten.distinct

    (keys, values)
  }

  private def unfeature(
    keys: Seq[String],
    values: Seq[Value],
    geomType: vt.Tile.GeomType,
    cmds: Seq[Command],
    data: Map[String, Value]
  ): vt.Tile.Feature = {
    val tags = data.toSeq.foldRight(List.empty[Int]) { case (pair, acc) =>
      keys.indexOf(pair._1) :: values.indexOf(pair._2) :: acc
    }

    vt.Tile.Feature(None, tags, Some(geomType), Command.uncommands(cmds))
  }
}
