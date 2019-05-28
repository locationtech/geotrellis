package geotrellis.doc.examples.spark

import geotrellis.layers.{LayerId, TileLayerMetadata}

object COGSparkExamples {
  def `Having an RDD[(ProjectedExtent, Tile)] ingest it as a Structured COG layer and query it` = {
    import geotrellis.raster._
    import geotrellis.raster.io.geotiff._
    import geotrellis.raster.resample._
    import geotrellis.tiling.{SpatialKey, KeyBounds, FloatingLayoutScheme}
    import geotrellis.spark._
    import geotrellis.spark.store._
    import geotrellis.layers.index.ZCurveKeyIndexMethod
    import geotrellis.layers._
    import geotrellis.layers.file._
    import geotrellis.layers.file.cog._
    import geotrellis.spark.store.file.cog._
    import geotrellis.spark.tiling.Tiler
    import geotrellis.vector._
    import geotrellis.proj4.WebMercator
    import geotrellis.raster.io.geotiff.writer.GeoTiffWriter

    import org.apache.spark.HashPartitioner
    import org.apache.spark.rdd.RDD
    import org.apache.spark.SparkContext

    // For the COGLayerWriter there should be a SparkContext in scope
    implicit val sc: SparkContext = ???

    // For instance we already loaded initial datasource into Spark memory
    // It can be done via HadoopGeoTiffRDD / S3GeoTiffRDD functions
    val rdd: RDD[(ProjectedExtent, Tile)] = ???

    // Tile this RDD to a grid layout. This will transform our raster data into a
    // common grid format, and merge any overlapping data.

    // We'll be tiling to a 512 x 512 tile size, and using the RDD's bounds as the tile bounds.
    val layoutScheme = FloatingLayoutScheme(512)

    // We gather the metadata that we will be targeting with the tiling here.
    // The return also gives us a zoom level
    val (zoom: Int, metadata: TileLayerMetadata[SpatialKey]) =
    rdd.collectMetadata[SpatialKey](layoutScheme)

    // Here we set some options for our tiling.
    // For this example, we will set the target partitioner to one
    // that has the same number of partitions as our original RDD.
    val tilerOptions =
    Tiler.Options(
      resampleMethod = Bilinear,
      partitioner = new HashPartitioner(rdd.partitions.length)
    )

    // Now we tile to an RDD with a SpaceTimeKey.

    val tiledRdd =
      rdd.tileToLayout[SpatialKey](metadata, tilerOptions)


    // At this point, we want to combine our RDD and our Metadata to get a TileLayerRDD[SpatialKey]

    val layerRdd: TileLayerRDD[SpatialKey] =
      ContextRDD(tiledRdd, metadata)

    // Create the attributes store that contains information about catalog.
    val attributeStore = FileAttributeStore("/path/to/catalog")

    // Create the writer instance, to write actual data into catalog.
    val writer = FileCOGLayerWriter(attributeStore)

    writer.write(
      layerName = "example_cog_layer",
      tiles = layerRdd,
      tileZoom = zoom, // actually the max desireable zoom, usually a current `layerRdd` zoom
      keyIndexMethod = ZCurveKeyIndexMethod // keyIndex to index these tiles
    )

    // Create the reader instance to query tiles stored as a Structured COG Layer
    val reader = FileCOGLayerReader(attributeStore)

    // Read layer at the max persisted zoom level
    // Actually it can be any zoom level in this case from the [0; zoom] values range
    val layer: TileLayerRDD[SpatialKey] = reader.read[SpatialKey, Tile](LayerId("example_cog_layer", zoom))

    // Let's stitch the layer into tile
    val raster: Raster[Tile] = layer.stitch

    // Create a tiff
    val tiff = GeoTiff(raster.reproject(layer.metadata.crs, WebMercator), WebMercator)

    // Write it in an optimized order
    GeoTiffWriter.write(tiff.crop(layer.metadata.extent), "/path/to/stitched/layer_chunk.tif", optimizedOrder = true)

    // It is also possible to perform queries
    val bounds1: KeyBounds[SpatialKey] = ???
    val bounds2: KeyBounds[SpatialKey] = ???

    // The result of a query
    val queryResult: TileLayerRDD[SpatialKey] =
      reader
        .query[SpatialKey, Tile](LayerId("example_cog_layer", zoom))
        .where(Intersects(bounds1) or Intersects(bounds2))
        .result

    // Let's create a ValueReader to query tiles by (x, y)
    val valueReader = FileCOGValueReader(attributeStore)

    // Behaviour of this API matches the common GeoTrellis layers API
    val layerValueReader = valueReader.reader[SpatialKey, Tile](LayerId("example_cog_layer", zoom))

    // Col and Row to query from the catalog
    val col: Int = ???
    val row: Int = ???

    // The result tile
    val resultTile: Tile = layerValueReader.read(SpatialKey(col, row))
  }

  def `Unstructured COG layer collection and persistence` = {
    // There is an example project: https://github.com/pomadchin/geotiff-layer
    // The idea of unstructured COGs it to use ANY GeoTiff set as a catalog
    // To do it there is still a requirement in metadata
    // A sort of a metadata should be collected to query GeoTiffs by extent
    // And to get some reference on it as the query result (for instance: URI)
    // Currently unstructured COG layer has only Collections API support

    import geotrellis.raster._
    import geotrellis.raster.resample._
    import geotrellis.raster.io.geotiff.Auto
    import geotrellis.tiling.ZoomedLayoutScheme
    import geotrellis.spark._
    import geotrellis.spark.store.file.geotiff._
    import geotrellis.proj4.WebMercator
    import java.net.URI

    // A path to an attribute store file
    // In this example it's a JSON file
    val mdJson: URI = ???

    // Path to some folder with tiffs
    val path: URI = ???

    // Let's generate the metadata for the layer
    val attributeStoreP =
      FileIMGeoTiffAttributeStore(
        name = "example_layer",
        uri = path
      )

    // It's also possible to persist it
    // Basically it was an Ingest Process
    // The result should be a file / table in some DB
    attributeStoreP.persist(mdJson)

    // Let's create another attribute store to read metadata from the JSON file
    // Though we can reuse the previous one as well
    val attributeStore = FileJsonGeoTiffAttributeStore(mdJson)

    // Create a GeoTiff Layer Reader
    // Strategy is a GDAL like strategy to pick up the best matching overview
    val geoTiffLayer = FileGeoTiffLayerReader(
      attributeStore = attributeStore,
      layoutScheme   = ZoomedLayoutScheme(WebMercator),
      resampleMethod = Bilinear,
      strategy       = Auto(0) // Auto(0) // AutoHigherResolution is the best matching ovr resolution
      // Auto(1) is a bit better than we need to grab (used this to correspond mapbox qualiy)
    )

    // Some desired zoom level
    // Raster would be up(dowb)sampled up to this zoom level
    // Taking into account the best matching overview
    val zoom: Int = ???

    // Spatial map coords
    val x: Int = ???
    val y: Int = ???

    // Read a Raster
    val red: Raster[Tile] = geoTiffLayer.read[Tile](LayerId("RED", zoom))(x, y)

    // Read the entire layer
    val layer: Traversable[Raster[Tile]] = geoTiffLayer.readAll[Tile](LayerId("RED", zoom))
  }
}
