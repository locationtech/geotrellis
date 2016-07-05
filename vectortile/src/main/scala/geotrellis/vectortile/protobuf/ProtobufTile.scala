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
import geotrellis.vectortile.{Layer, Value, VectorTile}
import scala.collection.mutable.ListBuffer
import vector_tile.{vector_tile => vt}

// --- //

case class ProtobufTile(
  layers: Map[String, ProtobufLayer]
) extends VectorTile

object ProtobufTile {
  /** Create a ProtobufTile masked as its parent trait. */
  def apply(tile: vt.Tile): VectorTile = {
    val layers: Map[String, ProtobufLayer] = tile.layers.map({ l =>
      val pbl = ProtobufLayer(l)

      pbl.name -> pbl
    }).toMap

    new ProtobufTile(layers)
  }
}

/**
 * Wild, unbased assumption of VT Features: their `id` values can be ignored
 * at read time, and rewritten as anything at write time.
 */
case class ProtobufLayer(
  rawLayer: vt.Tile.Layer
) extends Layer {
  /* Expected fields */
  def name: String = rawLayer.name
  def extent: Int = rawLayer.extent.getOrElse(4096)
  def version: Int = rawLayer.version

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

  // TODO Likely faster with manual recursion in a fold-like pattern,
  // and it will squash the pattern match warnings.
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
        case vt.Tile.GeomType.POINT => points.append(f)
        case vt.Tile.GeomType.LINESTRING => lines.append(f)
        case vt.Tile.GeomType.POLYGON => polys.append(f)
        case _ => Unit // `UNKNOWN` or `Unrecognized`.
      }
    }

    (points, lines, polys)
  }
}
