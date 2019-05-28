package geotrellis.doc.examples

object LandsatMultibandRDDExample {
  def main(args: Array[String]): Unit =
    `Generate an RDD of multiband tiles from landsat on S3`

  def `Generate an RDD of multiband tiles from landsat on S3`: Unit = {
    import geotrellis.raster._
    import geotrellis.spark._
    import geotrellis.spark.store.s3._
    import geotrellis.vector._

    import software.amazon.awssdk.services.s3.S3Client
    import org.apache.spark.SparkContext
    import org.apache.spark.rdd._

    import java.net.URI
    import scala.math.BigDecimal.RoundingMode

    implicit val sc: SparkContext =
      geotrellis.spark.util.SparkUtils.createLocalSparkContext("local[*]", "landsat-example")

    try {

      val options =
        S3GeoTiffRDD.Options(
          getClient = () => S3Client.create(),
          maxTileSize = Some(512),
          numPartitions = Some(100)
        )

      type LandsatKey = (ProjectedExtent, URI, Int)

      // For each RDD, we're going to include more information in the key, including:
      // - the ProjectedExtent
      // - the URI
      // - the future band value
      def uriToKey(bandIndex: Int): (URI, ProjectedExtent) => LandsatKey =
        { (uri, pe) =>
          (pe, uri, bandIndex)
        }

      // Read an RDD of source tiles for each of the bands.

      val redSourceTiles =
        S3GeoTiffRDD[ProjectedExtent, LandsatKey, Tile](
          "landsat-pds",
          "L8/139/045/LC81390452014295LGN00/LC81390452014295LGN00_B2.TIF",
          uriToKey(0),
          options
        )

      val greenSourceTiles =
        S3GeoTiffRDD[ProjectedExtent, LandsatKey, Tile](
          "landsat-pds",
          "L8/139/045/LC81390452014295LGN00/LC81390452014295LGN00_B3.TIF",
          uriToKey(1),
          options
        )

      val blueSourceTiles =
        S3GeoTiffRDD[ProjectedExtent, LandsatKey, Tile](
          "landsat-pds",
          "L8/139/045/LC81390452014295LGN00/LC81390452014295LGN00_B4.TIF",
          uriToKey(2),
          options
        )

      // Union these together, rearrange the elements so that we'll be able to group by key,
      // group them by key, and the rearrange again to produce multiband tiles.
      val sourceTiles: RDD[(ProjectedExtent, MultibandTile)] = {
        sc.union(redSourceTiles, greenSourceTiles, blueSourceTiles)
          .map { case ((pe, uri, bandIndex), tile) =>
            // Get the center of the tile, which we will join on
            val (x, y) = (pe.extent.center.x, pe.extent.center.y)

            // Round the center coordinates in case there's any floating point errors
            val center =
              (
                BigDecimal(x).setScale(5, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal(y).setScale(5, RoundingMode.HALF_UP).doubleValue()
              )

            // Get the scene ID from the path
            val sceneId = uri.getPath.split('/').reverse.drop(1).head

            val newKey = (sceneId, center)
            val newValue = (pe, bandIndex, tile)
            (newKey, newValue)
          }
          .groupByKey()
          .map { case (oldKey, groupedValues) =>
            val projectedExtent = groupedValues.head._1
            val bands = Array.ofDim[Tile](groupedValues.size)
            for((_, bandIndex, tile) <- groupedValues) {
              bands(bandIndex) = tile
            }

            (projectedExtent, MultibandTile(bands))
          }
      }

      // From here, you could ingest the multiband layer.
      // But for a simple test, we will rescale the bands and write them out to a single GeoTiff
      import geotrellis.tiling.{FloatingLayoutScheme, SpatialKey}
      import geotrellis.raster.io.geotiff.GeoTiff

      val (_, metadata) = sourceTiles.collectMetadata[SpatialKey](FloatingLayoutScheme(512))
      val tiles = sourceTiles.tileToLayout[SpatialKey](metadata)
      val raster =
        ContextRDD(tiles, metadata)
          .withContext { rdd =>
            rdd.mapValues { tile =>
              // Magic numbers! These were created by fiddling around with
              // numbers until some example landsat images looked good enough
              // to put on a map for some other project.
              val (min, max) = (4000, 15176)
              def clamp(z: Int) = {
                if(isData(z)) { if(z > max) { max } else if(z < min) { min } else { z } }
                else { z }
              }
              val red = tile.band(0).map(clamp _).delayedConversion(ByteCellType).normalize(min, max, 0, 255)
              val green = tile.band(1).map(clamp _).delayedConversion(ByteCellType).normalize(min, max, 0, 255)
              val blue = tile.band(2).map(clamp _).delayedConversion(ByteCellType).normalize(min, max, 0, 255)

              MultibandTile(red, green, blue)
            }
          }
          .stitch

      GeoTiff(raster, metadata.crs).write("/tmp/landsat-test.tif")
    } finally {
      sc.stop()
    }
  }

}
