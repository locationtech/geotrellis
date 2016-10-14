package geotrellis.osm

import geotrellis.util._
import geotrellis.vector._

import com.vividsolutions.jts.operation.linemerge.LineMerger
import com.vividsolutions.jts.geom.LineString
import org.apache.spark.rdd._

// --- //

class ElementToFeatureRDDMethods(val self: RDD[Element]) extends MethodExtensions[RDD[Element]] {
  def toFeatures: RDD[OSMFeature] = {

    /* All OSM Nodes */
    val nodes: RDD[Node] = self.flatMap({
      case e: Node => Some(e)
      case _ => None
    })

    /* All OSM Ways */
    val ways: RDD[Way] = self.flatMap({
      case e: Way  => Some(e)
      case _ => None
    })

    // Inefficient to do the flatMap thrice!
    // A function `split: RDD[Element] => (RDD[Node], RDD[Way], RDD[Relation])
    // would be nice.
    /* All OSM Relations */
    val relations: RDD[Relation] = self.flatMap({
      /* Baby steps. Limit to handling multipolys for now */
      case e: Relation if e.data.tagMap.get("type") == Some("multipolygon") => Some(e)
      case _ => None
    })

    val (points, lines, polys) = geometries(nodes, ways)

    val finalPolys: RDD[OSMFeature] =
      multipolygons(lines, polys, relations).asInstanceOf[RDD[OSMFeature]]

    points.asInstanceOf[RDD[OSMFeature]] ++ lines.asInstanceOf[RDD[OSMFeature]] ++ finalPolys
  }

  private def multipolygons(
    lines: RDD[Feature[Line, ElementData]],
    polys: RDD[Feature[Polygon, ElementData]],
    relations: RDD[Relation]
  ): RDD[Feature[Polygon, ElementData]] = {
    // filter out polys that are used in relations
    // merge RDDs back together

    val relLinks: RDD[(Long, Relation)] =
      relations.flatMap(r => r.members.map(m => (m.ref, r)))

    val lineLinks: RDD[(Long, Feature[Line, ElementData])] =
      lines.map(f => (f.data.meta.id, f))

    val grouped =
      polys.map(f => (f.data.meta.id, f)).cogroup(lineLinks, relLinks)

    val multipolys =
      grouped
        /* Assumption: Polygons and Lines exist in at most one "multipolygon" Relation */
        .flatMap({
          case (_, (ps, _, rs)) if !rs.isEmpty && !ps.isEmpty => Some((rs.head, Left(ps.head)))
          case (_, (_, ls, rs)) if !rs.isEmpty && !ls.isEmpty => Some((rs.head, Right(ls.head)))
          case _ => None
        })
        .groupByKey
        .map({ case (r, gs) =>

          /* Fuse Lines into Polygons */
          val ls: Vector[Feature[Line, ElementData]] = gs.flatMap({
            case Right(l) => Some(l)
            case _ => None
          }).toVector

          val ps: Vector[Feature[Polygon, ElementData]] = gs.flatMap({
            case Left(p) => Some(p)
            case _ => None
          }).toVector ++ fuseLines(spatialSort(ls))

//          val outerId: Long = r.members.filter(_.role == "outer").head.ref

//          val (Seq(outer), inners) = ps.partition(_.data.meta.id == outerId)

          /* It is suggested by OSM that multipoly tag data should be stored in
           * the Relation, not its constituent parts. Hence we take `r.data` here.
           *
           * However, "inner" Ways can have meaningful tags, such as a lake in
           * the middle of a forest.
           * TODO: We need a way to retain Hole tag data.
           *
           * Furthermore, winding order doesn't matter in OSM, but it does
           * in VectorTiles.
           * TODO: Make sure winding order is handled correctly.
           */
//          Feature(Polygon(outer.geom.exterior, inners.map(_.geom.exterior)), r.data)
        })


    polys
  }

  /** Order a given Vector of Features such that each Geometry is as
    * spatially close as possible to its neighbours in the result Vector.
    *
    * Time complexity: O(nlogn)
    */
  private def spatialSort(
    v: Vector[Feature[Line, ElementData]]
  ): Vector[Feature[Line, ElementData]] = v match {
    case v if v.length < 3 => v
    case v => {
      /* Kernels - Two points around which to group all others */
      val (kl, kr) = (v.head.geom.centroid.as[Point].get, v.last.geom.centroid.as[Point].get)

      /* Group all points spatially around the two kernels */
      val (l, r) = v.partition({ f =>
        val centre = f.geom.centroid.as[Point].get

        centre.distance(kl) < centre.distance(kr)
      })

      spatialSort(l) ++ spatialSort(r)
    }
  }

  /** ASSUMPTIONS:
    *   - Every Line in the given Vector can be fused
    *   - The final result of all fusions will be a set of Polygons
    *
    * Time complexity (raw): O(n^2)
    *
    * Time complexity (sorted): O(n)
    */
  private def fuseLines(
    v: Vector[Feature[Line, ElementData]]
  ): Vector[Feature[Polygon, ElementData]] = v match {
    case Vector() => Vector.empty
    case v if v.length == 1 => throw new IllegalArgumentException("Single unfusable Line remaining.")
    case v => {
      val (f, d, rest) = fuseOne(v)

      if (f.isClosed)
        Feature(Polygon(f), d) +: fuseLines(rest)
      else
        fuseLines(Feature(f, d) +: rest)
    }
  }

  /** Fuse the head Line in the Vector with the first other Line possible.
    * This borrows [[fuseLines]]'s assumptions.
    */
  private def fuseOne(
    v: Vector[Feature[Line, ElementData]]
  ): (Line, ElementData, Vector[Feature[Line, ElementData]]) = {
    val h = v.head
    val t = v.tail

    // TODO: Use a `while` instead?
    for ((f, i) <- t.zipWithIndex) {
      if (h.geom.touches(f.geom)) { /* Found two lines that should fuse */
        val lm = new LineMerger  /* from JTS */

        lm.add(h.geom.jtsGeom)
        lm.add(f.geom.jtsGeom)

        val line: Line = Line(lm.getMergedLineStrings.toArray(Array.empty[LineString]).head)

        val (a, b) = t.splitAt(i)

        /* Return early */
        return (line, h.data, a ++ b.tail)
      }
    }

    /* As every Line _must_ fuse, this should never be reached */
    ???
  }

  /** Every OSM Node and Way converted to GeoTrellis Geometries.
    * This includes Points, Lines, and Polygons which have no holes.
    * Holed polygons are handled by [[multipolygons]], as they are represented
    * by OSM Relations.
    */
  private def geometries(
    nodes: RDD[Node],
    ways: RDD[Way]
  ): (RDD[Feature[Point, ElementData]], RDD[Feature[Line, ElementData]], RDD[Feature[Polygon, ElementData]]) = {
    /* You're a long way from finishing this operation. */
    val links: RDD[(Long, Way)] = ways.flatMap(w => w.nodes.map(n => (n, w)))

    val grouped: RDD[(Long, (Iterable[Node], Iterable[Way]))] =
      nodes.map(n => (n.data.meta.id, n)).cogroup(links)

    val linesPolys: RDD[Either[Feature[Line, ElementData], Feature[Polygon, ElementData]]] =
      grouped
        .flatMap({ case (_, (ns, ws)) =>
          val n = ns.head

          ws.map(w => (w, n))
        })
        .groupByKey
        .map({ case (w, ns) =>
          /* De facto maximum of 2000 Nodes */
          val sorted: Vector[Node] = ns.toVector.sortBy(n => n.data.meta.id)

          /* `get` is safe, the BTree is guaranteed to be populated */
          val tree: BTree[Node] = BTree.fromSortedSeq(sorted).get

          /* A binary search branch predicate */
          val pred: (Long, BTree[Node]) => Either[Option[BTree[Node]], Node] = { (n, tree) =>
            if (n == tree.value.data.meta.id) {
              Right(tree.value)
            } else if (n < tree.value.data.meta.id) {
              Left(tree.left)
            } else {
              Left(tree.right)
            }
          }

          /* The actual node coordinates in the correct order */
          val points: Vector[(Double, Double)] =
            w.nodes
              .flatMap(n => tree.searchWith(n, pred))
              .map(n => (n.lat, n.lon))

          if (w.isLine) {
            Left(Feature(Line(points), w.data))
          } else {
            Right(Feature(Polygon(points), w.data))
          }
        })

    // TODO: More inefficient RDD splitting.
    val lines: RDD[Feature[Line, ElementData]] = linesPolys.flatMap({
      case Left(l) => Some(l)
      case _ => None
    })

    val polys: RDD[Feature[Polygon, ElementData]] = linesPolys.flatMap({
      case Right(p) => Some(p)
      case _ => None
    })

    /* Single Nodes unused in any Way */
    val points: RDD[Feature[Point, ElementData]] = grouped.flatMap({ case (_, (ns, ws)) =>
      if (ws.isEmpty) {
        val n = ns.head

        Some(Feature(Point(n.lat, n.lon), n.data))
      } else {
        None
      }
    })

    (points, lines, polys)
  }
}
