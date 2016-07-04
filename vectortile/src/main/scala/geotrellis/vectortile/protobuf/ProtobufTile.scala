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
import geotrellis.vectortile.{ Layer, Value, VectorTile }
import scala.collection.mutable.ListBuffer
import vector_tile.{ vector_tile => vt }

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

/** Wild, unbased assumption of VT Features: their `id` values can be ignored
  * at read time, and rewritten as anything at write time.
  */
case class ProtobufLayer(
  name: String,
  extent: Int,
  rawFeatures: Seq[vt.Tile.Feature]
) extends Layer {
  /* Unconsumed raw Features */
  private val (pointFs, lineFs, polyFs) = segregate(rawFeatures)

  // TODO Use `trimStart` for shaving off elements

  lazy val (points, multiPoints): (Stream[Feature[Point, Map[String, Value]]], Stream[Feature[MultiPoint, Map[String, Value]]]) = ???

  lazy val (lines, multiLines): (Stream[Feature[Line, Map[String, Value]]], Stream[Feature[MultiLine, Map[String, Value]]]) = ???

  lazy val (polygons, multiPolygons): (Stream[Feature[Polygon, Map[String, Value]]], Stream[Feature[MultiPolygon, Map[String, Value]]]) = ???

//  def points: Seq[Feature[Point, Map[String, Value]]]
//  def multiPoints: Seq[Feature[MultiPoint, Map[String, Value]]]
//  def lines: Seq[Feature[Line, Map[String, Value]]] = ???
//  def multiLines: Seq[Feature[MultiLine, Map[String, Value]]] = ???
//  def polygons: Seq[Feature[Polygon, Map[String, Value]]] = ???
//  def multiPolygons: Seq[Feature[MultiPolygon, Map[String, Value]]] = ???

  def allGeometries: Seq[Feature[Geometry, Map[String, Value]]] = {
    Seq.empty[Feature[Geometry, Map[String, Value]]]
  }

  /** Given a raw protobuf Layer, segregate its Features by their GeomType.
    * `UNKNOWN` geometry types are ignored.
    */
  private def segregate(
    features: Seq[vt.Tile.Feature]
  ): (ListBuffer[vt.Tile.Feature], ListBuffer[vt.Tile.Feature], ListBuffer[vt.Tile.Feature]) = {
    val points = new ListBuffer[vt.Tile.Feature]
    val lines = new ListBuffer[vt.Tile.Feature]
    val polys = new ListBuffer[vt.Tile.Feature]

    features.foreach { f => f.getType match {
      case vt.Tile.GeomType.POINT => points.append(f)
      case vt.Tile.GeomType.LINESTRING => lines.append(f)
      case vt.Tile.GeomType.POLYGON => polys.append(f)
      case _ => Unit  // `UNKNOWN` or `Unrecognized`.
    }}

    (points, lines, polys)
  }

  /** Force all the internal raw Feature stores to fully parse their contents
    * into Geotrellis Features.
    */
  def force: Unit = {
    ???
  }
}

object ProtobufLayer {
  def apply(layer: vt.Tile.Layer): ProtobufLayer = ???
}
