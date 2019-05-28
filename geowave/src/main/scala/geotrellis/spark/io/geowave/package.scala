package geotrellis.spark.store

import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.io.WKTWriter
import com.vividsolutions.jts.io.{WKTReader => OLDWKTReader}
import com.vividsolutions.jts.{geom => jtsOld}

package object geowave {
  /** An ugly conversion function from the new jts.Geometry type into the old GeoWave compatible Geometry type */
  implicit def geometryConversion(geom: jts.Geometry): jtsOld.Geometry =
    new OLDWKTReader().read(new WKTWriter().write(geom))
}
