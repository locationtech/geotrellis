package geotrellis.spark.etl.config

import geotrellis.proj4.CRS
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutScheme, ZoomedLayoutScheme}

case class IngestLayoutScheme(`type`: Option[String], crs: CRS, tileSize: Int, resolutionThreshold: Double) {
  lazy val getLayoutScheme: Option[LayoutScheme] = `type` map (_ match {
    case "floating" => FloatingLayoutScheme(tileSize)
    case "zoomed"   => ZoomedLayoutScheme(crs, tileSize, resolutionThreshold)
    case _          => throw new Exception("unsupported layout scheme definition")
  })
}
