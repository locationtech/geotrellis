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
import scala.collection.mutable.ListBuffer
import vector_tile.{vector_tile => vt}
import vector_tile.vector_tile.Tile.GeomType.{POINT, LINESTRING, POLYGON}

// --- //

/**
  * A concrete representation of a VectorTile, as one decoded from Protobuf
  * bytes. This is the original/default type of VectorTile.
  *
  * {{{
  * import geotrellis.vectortile.protobuf._
  *
  * val bytes: Array[Byte] = ...
  * val tile: VectorTile = ProtobufTile.fromBytes(bytes)
  * }}}
  *
  * @constructor This is not meant to be called directly. See this class's
  * companion object for the available helper methods.
  */
case class ProtobufTile(
  layers: Map[String, ProtobufLayer]
) extends VectorTile {
  /** Encode this VectorTile back into a mid-level Protobuf object. */
  def toProtobuf: vt.Tile =
    vt.Tile(layers = layers.values.map(_.toProtobuf).toSeq)

  /** Encode this VectorTile back into its original form of Protobuf bytes. */
  def toBytes: Array[Byte] = toProtobuf.toByteArray
}

object ProtobufTile {
  /** Create a ProtobufTile masked as its parent trait. */
  def fromPBTile(tile: vt.Tile): VectorTile = {
    val layers: Map[String, ProtobufLayer] = tile.layers.map({ l =>
      val pbl = ProtobufLayer(l)

      pbl.name -> pbl
    }).toMap

    new ProtobufTile(layers)
  }

  /** Create a [[VectorTile]] from raw Protobuf bytes. */
  def fromBytes(bytes: Array[Byte]): VectorTile = {
    fromPBTile(vt.Tile.parseFrom(bytes))
  }
}

/**
  * A [[Layer]] decoded from Protobuf data. All of its Features are decoded
  * lazily, making for very fast extraction of single features/geometries.
  *
  */
// Wild, unbased assumption of VT Features: their `id` values can be ignored
// at read time, and rewritten as anything at write time.
case class ProtobufLayer(
  private val rawLayer: vt.Tile.Layer
) extends Layer {
  /* Expected fields */
  def name: String = rawLayer.name
  def extent: Int = rawLayer.extent.getOrElse(4096)

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
  private val (pointFs, lineFs, polyFs) = segregate(rawLayer.features)

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
        val g = protobufGeom.fromCommands(Command.commands(geoms))

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

  /* OPTIMIZATION NOTE
   * These lazy vals produce pattern match warnings, but it's okay to ignore
   * them. Below is the initial attempt at the algorithm. To avoid the warning
   * and hopefully improve performance, replacing `map . filter` with `foldLeft`
   * was later attempted. It turns out `foldLeft` is about 20% slower, so we've
   * kept the original implementation.
   */
  lazy val points: Stream[Feature[Point, Map[String, Value]]] = pointStream
    .filter(_._1.isLeft)
    .map({ case (Left(p), meta) => Feature(p, meta) })

  lazy val multiPoints: Stream[Feature[MultiPoint, Map[String, Value]]] = pointStream
    .filter(_._1.isRight)
    .map({ case (Right(p), meta) => Feature(p, meta) })

  lazy val lines: Stream[Feature[Line, Map[String, Value]]] = lineStream
    .filter(_._1.isLeft)
    .map({ case (Left(p), meta) => Feature(p, meta) })

  lazy val multiLines: Stream[Feature[MultiLine, Map[String, Value]]] = lineStream
    .filter(_._1.isRight)
    .map({ case (Right(p), meta) => Feature(p, meta) })

  lazy val polygons: Stream[Feature[Polygon, Map[String, Value]]] = polyStream
    .filter(_._1.isLeft)
    .map({ case (Left(p), meta) => Feature(p, meta) })

  lazy val multiPolygons: Stream[Feature[MultiPolygon, Map[String, Value]]] = polyStream
    .filter(_._1.isRight)
    .map({ case (Right(p), meta) => Feature(p, meta) })

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
    // TODO Where should these be?
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
      points.map(f => unfeature(keys, values, POINT, pgp.toCommands(Left(f.geom)), f.data)),
      multiPoints.map(f => unfeature(keys, values, POINT, pgp.toCommands(Right(f.geom)), f.data)),
      lines.map(f => unfeature(keys, values, LINESTRING, pgl.toCommands(Left(f.geom)), f.data)),
      multiLines.map(f => unfeature(keys, values, LINESTRING, pgl.toCommands(Right(f.geom)), f.data)),
      polygons.map(f => unfeature(keys, values, POLYGON, pgy.toCommands(Left(f.geom)), f.data)),
      multiPolygons.map(f => unfeature(keys, values, POLYGON, pgy.toCommands(Right(f.geom)), f.data))
    ).flatten

    vt.Tile.Layer(version, name, features, keys, values.map(_.unval), Some(extent))
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
