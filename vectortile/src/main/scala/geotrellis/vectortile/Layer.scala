/*
 * Copyright 2019 Azavea
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

import geotrellis.vectortile.internal._
import geotrellis.vectortile.internal.PBTile._
import geotrellis.vectortile.internal.PBTile.PBGeomType.{LINESTRING, POINT, POLYGON}

import geotrellis.util.annotations.experimental
import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.vector._

import scala.collection.mutable.ListBuffer

// --- //

/** A layer, which could contain any number of Features of any Geometry type.
  * Here, "Feature" and "Geometry" refer specifically to the GeoTrellis classes
  * of the same names.
  */
@experimental trait Layer extends java.io.Serializable {
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
  def points: Seq[MVTFeature[Point]]
  /** Every MultiPoint Feature in this Layer. */
  def multiPoints: Seq[MVTFeature[MultiPoint]]
  /** Every Line Feature in this Layer. */
  def lines: Seq[MVTFeature[LineString]]
  /** Every MultiLine Feature in this Layer. */
  def multiLines: Seq[MVTFeature[MultiLineString]]
  /** Every Polygon Feature in this Layer. */
  def polygons: Seq[MVTFeature[Polygon]]
  /** Every MultiPolygon Feature in this Layer. */
  def multiPolygons: Seq[MVTFeature[MultiPolygon]]

  /** All Features of Single and Multi Geometries. */
  def features: Seq[MVTFeature[Geometry]] = {
    Seq(
      points,
      multiPoints,
      lines,
      multiLines,
      polygons,
      multiPolygons
    ).flatten
  }

  /**
    * Encode this ProtobufLayer a mid-level Layer ready to be encoded as protobuf bytes.
    * @param forcePolygonWinding is a parameter to force orient all Polygons and MultiPolygons
    *                           clockwise, since it's a MapBox spec requirement:
    *                           Any polygon interior ring must be oriented with the winding order opposite that of their
    *                           parent exterior ring and all interior rings must directly follow the exterior ring to which they belong.
    *                           Exterior rings must be oriented clockwise and interior rings must be oriented counter-clockwise (when viewed in screen coordinates).
    *                           See https://docs.mapbox.com/vector-tiles/specification/#winding-order for mor details.
    **/
  private[vectortile] def toProtobuf(forcePolygonWinding: Boolean = true): PBLayer = {
    val pgp = implicitly[ProtobufGeom[Point, MultiPoint]]
    val pgl = implicitly[ProtobufGeom[LineString, MultiLineString]]
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
      points.map(f => unfeature(f.id, keyMap, valMap, POINT, pgp.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiPoints.map(f => unfeature(f.id, keyMap, valMap, POINT, pgp.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data)),
      lines.map(f => unfeature(f.id, keyMap, valMap, LINESTRING, pgl.toCommands(Left(f.geom), tileExtent.northWest, resolution), f.data)),
      multiLines.map(f => unfeature(f.id, keyMap, valMap, LINESTRING, pgl.toCommands(Right(f.geom), tileExtent.northWest, resolution), f.data)),
      polygons.map { f =>
        val geom = if(forcePolygonWinding) f.geom.normalized() else f.geom
        unfeature(f.id, keyMap, valMap, POLYGON,
                  pgy.toCommands(Left(geom), tileExtent.northWest, resolution), f.data)
      },
      multiPolygons.map { f =>
        val geom = if(forcePolygonWinding) f.geom.normalized() else f.geom
        unfeature(f.id, keyMap, valMap, POLYGON,
                  pgy.toCommands(Right(geom), tileExtent.northWest, resolution), f.data)
      }
    ).flatten

    PBLayer(version, name, features, keys, values.map(_.toProtobuf), Some(tileWidth))
  }

  private def totalMeta: (Seq[String], Seq[Value]) = {
    /* Pull into memory once to avoid GC on the feature list */
    val fs: Seq[MVTFeature[Geometry]] = features

    /* Must be unique */
    val keys: Seq[String] = fs.flatMap(_.data.keys).distinct

    val values: Seq[Value] = fs.flatMap(_.data.values).distinct

    (keys, values)
  }

  private def unfeature(
    featureId: Option[Long],
    keys: Map[String, Int],
    values: Map[Value, Int],
    geomType: PBGeomType,
    cmds: Seq[Command],
    data: Map[String, Value]
  ): PBFeature = {
    val tags = data.toSeq.foldRight(List.empty[Int]) { case (pair, acc) =>
      /* These `Map.apply` _should_ never fail */
      keys(pair._1) :: values(pair._2) :: acc
    }

    PBFeature(featureId, tags, Some(geomType), Command.uncommands(cmds))
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

  private def prettyFeature[G <: Geometry](fs: Seq[MVTFeature[G]]): String = {
    if (fs.isEmpty) "}" else {
      fs.map({ f =>
s"""
        feature {
          id = ${f.id}
          geometry (WKT) = ${f.geom}
          geometry (LatLng GeoJson) = ${f.geom.reproject(WebMercator, LatLng).toGeoJson()}
          ${prettyMeta(f.data)}
        }
"""
      }).mkString("\n") ++ "      }"
    }
  }

  private def prettyMeta(meta: Map[String, Value]): String = {
    if (meta.isEmpty) "metadata {}" else {
      val sortedMeta = meta.toSeq.sortBy(_._1)

      s"""metadata { ${sortedMeta.map({ case (k,v) => s"            ${k}: ${v}"}).mkString("\n")}}"""
    }
  }
}

/** A [[Layer]] crafted through some strict ingest process. */
@experimental case class StrictLayer(
  name: String,
  tileWidth: Int,
  version: Int,
  tileExtent: Extent,
  mvtFeatures: MVTFeatures
) extends Layer {
  def points = mvtFeatures.points
  def multiPoints = mvtFeatures.multiPoints
  def lines = mvtFeatures.lines
  def multiLines = mvtFeatures.multiLines
  def polygons = mvtFeatures.polygons
  def multiPolygons = mvtFeatures.multiPolygons
}

object StrictLayer {
  def apply(
    name: String,
    tileWidth: Int,
    version: Int,
    tileExtent: Extent,
    points: Seq[MVTFeature[Point]],
    multiPoints: Seq[MVTFeature[MultiPoint]],
    lines: Seq[MVTFeature[LineString]],
    multiLines: Seq[MVTFeature[MultiLineString]],
    polygons: Seq[MVTFeature[Polygon]],
    multiPolygons: Seq[MVTFeature[MultiPolygon]]
  ): StrictLayer = StrictLayer(
    name, tileWidth, version, tileExtent,
    MVTFeatures(
      points,
      multiPoints,
      lines,
      multiLines,
      polygons,
      multiPolygons
    )
  )
}

/**
  * A [[Layer]] decoded from Protobuf data. All of its Features are decoded
  * lazily, making for very fast extraction of single features/geometries.
  *
  */
@experimental case class LazyLayer(
  private val rawLayer: PBLayer,
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
  private def geomStream[G1 <: Geometry, G2 <: Geometry](
    feats: ListBuffer[PBFeature]
  )(implicit protobufGeom: ProtobufGeom[G1, G2]): Stream[(Option[Long], Either[G1, G2], Map[String, Value])] = {
    def loop(fs: ListBuffer[PBFeature]): Stream[(Option[Long], Either[G1, G2], Map[String, Value])] = {
      if (fs.isEmpty) {
        Stream.empty[(Option[Long], Either[G1, G2], Map[String, Value])]
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
          (fs.head.id, g, getMeta(rawLayer.keys, rawLayer.values, fs.head.tags)) #:: loop(fs.tail)
        }
      }
    }

    loop(feats)
  }

  /**
   * Construct Feature-specific metadata from the key/value lists of
   * the parent layer.
   */
  private def getMeta(keys: Seq[String], vals: Seq[PBValue], tags: Seq[Int]): Map[String, Value] = {
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
  private lazy val lineStream = geomStream[LineString, MultiLineString](lineFs)
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
  lazy val points: Stream[MVTFeature[Point]] = pointStream
    .flatMap({
      case (id, Left(p), meta) => new ::(MVTFeature(id, p, meta), Nil)
      case _ => Nil
    })

  lazy val multiPoints: Stream[MVTFeature[MultiPoint]] = pointStream
    .flatMap({
      case (id, Right(p), meta) if !p.isEmpty => new ::(MVTFeature(id, p, meta), Nil)
      case _ => Nil
    })

  lazy val lines: Stream[MVTFeature[LineString]] = lineStream
    .flatMap({
      case (id, Left(p), meta) if !p.isEmpty => new ::(MVTFeature(id, p, meta), Nil)
      case _ => Nil
    })

  lazy val multiLines: Stream[MVTFeature[MultiLineString]] = lineStream
    .flatMap({
      case (id, Right(p), meta) if !p.isEmpty => new ::(MVTFeature(id, p, meta), Nil)
      case _ => Nil
    })

  lazy val polygons: Stream[MVTFeature[Polygon]] = polyStream
    .flatMap({
      case (id, Left(p), meta) if !p.isEmpty => new ::(MVTFeature(id, p, meta), Nil)
      case _ => Nil
    })

  lazy val multiPolygons: Stream[MVTFeature[MultiPolygon]] = polyStream
    .flatMap({
      case (id, Right(p), meta) if !p.isEmpty => new ::(MVTFeature(id, p, meta), Nil)
      case _ => Nil
    })

  /**
   * Given a raw protobuf Layer, segregate its Features by their GeomType.
   * `UNKNOWN` geometry types are ignored.
   */
  private def segregate(
    features: Seq[PBFeature]
  ): (ListBuffer[PBFeature], ListBuffer[PBFeature], ListBuffer[PBFeature]) = {
    val points = new ListBuffer[PBFeature]
    val lines = new ListBuffer[PBFeature]
    val polys = new ListBuffer[PBFeature]

    features.foreach { f =>
      f.getType match {
        case POINT => points += f
        case LINESTRING => lines += f
        case POLYGON => polys += f
        case _ => () // `UNKNOWN` or `Unrecognized`.
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
      MVTFeatures(
        points,
        multiPoints,
        lines,
        multiLines,
        polygons,
        multiPolygons
      )
    )
  }
}
