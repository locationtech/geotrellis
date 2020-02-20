package geotrellis.vector

/**
 * An object containing duplicate geometry objects for construction of geometries
 * in the REPL environment.
 *
 * This object exists to work around a REPL bug which masks objects when instances
 * of classes with the same name are created.  DO NOT USE IN COMPILED CODE.  Use bare
 * objects instead (Point, LineString, etc).
 */
object JTS {
  object Point extends PointConstructors
  object LineString extends LineStringConstructors
  object Polygon extends PolygonConstructors
  object MultiPoint extends MultiPointConstructors
  object MultiLineString extends MultiLineStringConstructors
  object MultiPolygon extends MultiPolygonConstructors
  object GeometryCollection extends GeometryCollectionConstructors
}
