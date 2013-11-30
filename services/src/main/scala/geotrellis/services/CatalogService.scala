package geotrellis.service

import geotrellis._
import geotrellis.service.Json._


object CatalogService {
  def getJson() =
    GeoTrellis.server.catalog.toJson
}
