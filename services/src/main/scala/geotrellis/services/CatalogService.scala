package geotrellis.service

import geotrellis._
import geotrellis.service.Json._


object CatalogService {
  def asJson() =
    GeoTrellis.server.catalog.toJson
}
