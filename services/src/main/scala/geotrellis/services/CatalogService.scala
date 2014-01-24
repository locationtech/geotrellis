package geotrellis.services

import geotrellis._
import geotrellis.services.Json._


object CatalogService {
  def getJson() =
    GeoTrellis.server.catalog.toJson
}
