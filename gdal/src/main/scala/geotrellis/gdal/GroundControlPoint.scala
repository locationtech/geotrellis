package geotrellis.gdal

import org.gdal.gdal.GCP

case class GroundControlPoint(id: String,
                              info: String,
                              col: Double,
                              row: Double,
                              x: Double,
                              y: Double,
                              z: Double)

object GroundControlPoint {
  def apply(gcp: GCP): GroundControlPoint =
    GroundControlPoint(gcp.getId,
                       gcp.getInfo,
                       gcp.getGCPPixel,
                       gcp.getGCPLine,
                       gcp.getGCPX,
                       gcp.getGCPY,
                       gcp.getGCPZ)
}
