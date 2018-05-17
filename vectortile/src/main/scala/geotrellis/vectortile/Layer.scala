/*
 * Copyright 2016 Azavea
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

package geotrellis.vectortile

import scala.collection.mutable.ListBuffer

import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vectortile.internal.{vector_tile => vt, _}
import geotrellis.vectortile.internal.vector_tile.Tile.GeomType.{LINESTRING, POINT, POLYGON}
import geotrellis.util.annotations.experimental

// --- //

/** A layer, which could contain any number of Features of any Geometry type.
  * Here, "Feature" and "Geometry" refer specifically to the GeoTrellis classes
  * of the same names.
  */
@experimental trait Layer extends Serializable {
  /** The VectorTile spec version that this Layer obeys. */
  def version: Int

  /** The layer's name. */
  def name: String

  /** The GeoTrellis Extent of this Layer's parent [[VectorTile]]. */
  def tileExtent: Extent

  /** The width/height of this Layer's coordinate grid. By default this is 4096,
    * as per the VectorTile specification.
    *
    * Referred to as ''extent'' in the spec, but we opt for a different name
    * to avoid confusion with a GeoTrellis [[Extent]].
    */
  def tileWidth: Int

  /** How much of the [[Extent]] is covered by a single grid coordinate? */
  def resolution: Double = tileExtent.height / tileWidth

  /** Every Point Feature in this Layer. */
  def points: Seq[Feature[Point, Map[String, Value]]]
  /** Every MultiPoint Feature in this Layer. */
  def multiPoints: Seq[Feature[MultiPoint, Map[String, Value]]]
  /** Every Line Feature in this Layer. */
  def lines: Seq[Feature[Line, Map[String, Value]]]
  /** Every MultiLine Feature in this Layer. */
  def multiLines: Seq[Feature[MultiLine, Map[String, Value]]]
  /** Every Polygon Feature in this Layer. */
  def polygons: Seq[Feature[Polygon, Map[String, Value]]]
  /** Every MultiPolygon Feature in this Layer. */
  def multiPolygons: Seq[Feature[MultiPolygon, Map[String, Value]]]

  /** All Features of Single and Multi Geometries. */
  def features: Seq[Feature[Geometry, Map[String, Value]]] = {
    Seq(
      points,
      multiPoints,
      lines,
      multiLines,
      polygons,
      multiPolygons
    ).flatten
  }

  /** Encode this ProtobufLayer a mid-level Layer ready to be encoded as protobuf bytes. */
  private[vectortile] def toProtobuf: vt.Tile.Layer = {
    val pgp = implicitly[ProtobufGeom[Point, MultiPoint]]
    val pgl = implicitly[ProtobufGeom[Line, MultiLine]]
    val pgy = implicitly[ProtobufGeom[Polygon, MultiPolygon]]

    val (keys, values) = totalMeta

    /* Construct Maps of keys and values with their Seq indices, so that
     * lookups in `unfeature` will be fast. Previously they were using
     * `Seq.indexOf`, which turned out to be O(n^2) for Analytic vectortiles.
     */
    val keyMap: Map[String, Int] = keys.zipWithIndex.toMap
    val valMap: Map[Value, Int] = values.zipWithIndex.toMap

    /* In a future version of the VectorTile spec, when Single and Multi
     * Geometries are separate, we will be able to restructre `ProtobufGeom`
     * in such a way that makes `Geometry.toCommands` possible here.
     *
     * `unfeature` will become polymorphic, so calls to it will look like:
     *
     *   points.map(f => unfeature(keys, values, f))
     */
    val features = Seq(
      points.map(f => unfeature(keyMap, valMap, POINT, pgp.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiPoints.map(f => unfeature(keyMap, valMap, POINT, pgp.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data)),
      lines.map(f => unfeature(keyMap, valMap, LINESTRING, pgl.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiLines.map(f => unfeature(keyMap, valMap, LINESTRING, pgl.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data)),
      polygons.map(f => unfeature(keyMap, valMap, POLYGON, pgy.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiPolygons.map(f => unfeature(keyMap, valMap, POLYGON, pgy.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data))
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
    keys: Map[String, Int],
    values: Map[Value, Int],
    geomType: vt.Tile.GeomType,
    cmds: Seq[Command],
    data: Map[String, Value]
  ): vt.Tile.Feature = {
    val tags = data.toSeq.foldRight(List.empty[Int]) { case (pair, acc) =>
      /* These `Option.get` _should_ never fail */
      keys.get(pair._1).get :: values.get(pair._2).get :: acc
    }

    vt.Tile.Feature(None, tags, Some(geomType), Command.uncommands(cmds))
  }

  /** Pretty-print this `Layer`. */
  def pretty: String = {
    s"""
  layer ${name} {
    version       = ${version}
    vt_resolution = ${tileWidth}
    tile_extent   = ${tileExtent}

    features {
      points (${points.length}) {${prettyFeature(points)}

      multiPoints (${multiPoints.length}) {${prettyFeature(multiPoints)}

      lines (${lines.length}) {${prettyFeature(lines)}

      multiLines (${multiLines.length}) {${prettyFeature(multiLines)}

      polygons (${polygons.length}) {${prettyFeature(polygons)}

      multiPolygons (${multiPolygons.length}) {${prettyFeature(multiPolygons)}
    }
  }
"""
  }

  private def prettyFeature[G <: Geometry](fs: Seq[Feature[G, Map[String, Value]]]): String = {
    if (fs.isEmpty) "}" else {
      fs.map({ f =>
s"""
        feature {
          geometry (WKT) = ${f.geom}
          geometry (LatLng GeoJson) = ${f.geom.reproject(WebMercator, LatLng).toGeoJson}
          ${prettyMeta(f.data)}
        }
"""
      }).mkString("\n") ++ "      }"
    }
  }

  private def prettyMeta(meta: Map[String, Value]): String = {
    if (meta.isEmpty) "metadata {}" else {
      val sortedMeta = meta.toSeq.sortBy(_._1)

      s"""
          metadata {
${sortedMeta.map({ case (k,v) => s"            ${k}: ${v}"}).mkString("\n")}
          }"""
    }
  }
}

/** A [[Layer]] crafted through some strict ingest process. */
@experimental case class StrictLayer(
  name: String,
  tileWidth: Int,
  version: Int,
  tileExtent: Extent,
  points: Seq[Feature[Point, Map[String, Value]]],
  multiPoints: Seq[Feature[MultiPoint, Map[String, Value]]],
  lines: Seq[Feature[Line, Map[String, Value]]],
  multiLines: Seq[Feature[MultiLine, Map[String, Value]]],
  polygons: Seq[Feature[Polygon, Map[String, Value]]],
  multiPolygons: Seq[Feature[MultiPolygon, Map[String, Value]]]
) extends Layer

/**
  * A [[Layer]] decoded from Protobuf data. All of its Features are decoded
  * lazily, making for very fast extraction of single features/geometries.
  *
  */
@experimental case class LazyLayer(
  private val rawLayer: vt.Tile.Layer,
  tileExtent: Extent
) extends Layer {
  /* Expected fields */
  def name: String = rawLayer.name
  def tileWidth: Int = rawLayer.extent.getOrElse(4096)

  /** The version of the specification that this Layer adheres to. */
  def version: Int = rawLayer.version

  /* Unconsumed raw Features */
  private lazy val (pointFs, lineFs, polyFs) = segregate(rawLayer.features)

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
        val geoms: Seq[Int] = fs.head.geometry

        /* 2017 May  1 @ 14:56
         * There is a strange bug where a Feature is being parsed out of
         * some Protobuf data, but that Feature has no geometries. This should never
         * happen, but in the wild it seems to be (on a tile set that I injested
         * myself.) This needs to be looked into.
         *
         * The `if` here is a workaround that ignores a Feature with no geoms.
         */
        if (geoms.isEmpty) {
          loop(fs.tail)
        } else {
          val g = protobufGeom.fromCommands(Command.commands(geoms), tileExtent.northWest, resolution)

          (g, getMeta(rawLayer.keys, rawLayer.values, fs.head.tags)) #:: loop(fs.tail)
        }
      }
    }

    loop(feats)
  }

  /**
   * Construct Feature-specific metadata from the key/value lists of
   * the parent layer.
   */
  private def getMeta(keys: Seq[String], vals: Seq[vt.Tile.Value], tags: Seq[Int]): Map[String, Value] = {
    /* The Seqs passed in here are backed by [[Vector]] on the Protobuf
     * end of things.
     */
    tags
      .grouped(2)
      .map({ case Vector(k, v) => keys(k) -> protoVal(vals(v)) })
      .toMap
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
   *
   * BUG NOTES
   * The `p.isEmpty` check is done here to ignore any empty Geometries, which is
   * a legal state for JTS Geoms. These cause problems later when reading/writing
   * VT Features, so we avoid those problems by ignoring any empty Geoms here.
   */
  lazy val points: Stream[Feature[Point, Map[String, Value]]] = pointStream
    .flatMap({
      case (Left(p), meta) => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val multiPoints: Stream[Feature[MultiPoint, Map[String, Value]]] = pointStream
    .flatMap({
      case (Right(p), meta) if !p.isEmpty => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val lines: Stream[Feature[Line, Map[String, Value]]] = lineStream
    .flatMap({
      case (Left(p), meta) if !p.isEmpty => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val multiLines: Stream[Feature[MultiLine, Map[String, Value]]] = lineStream
    .flatMap({
      case (Right(p), meta) if !p.isEmpty => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val polygons: Stream[Feature[Polygon, Map[String, Value]]] = polyStream
    .flatMap({
      case (Left(p), meta) if !p.isEmpty => new ::(Feature(p, meta), Nil)
      case _ => Nil
    })

  lazy val multiPolygons: Stream[Feature[MultiPolygon, Map[String, Value]]] = polyStream
    .flatMap({
      case (Right(p), meta) if !p.isEmpty => new ::(Feature(p, meta), Nil)
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
        case POINT => points += f
        case LINESTRING => lines += f
        case POLYGON => polys += f
        case _ => Unit // `UNKNOWN` or `Unrecognized`.
      }
    }

    (points, lines, polys)
  }

  /** Convert to a [[StrictLayer]]. */
  def toStrict: StrictLayer = {
    StrictLayer(
      name,
      tileWidth,
      version,
      tileExtent,
      points,
      multiPoints,
      lines,
      multiLines,
      polygons,
      multiPolygons
    )
  }
}
