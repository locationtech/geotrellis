# Examples

### Using a SpaceTimeKey -> SpatialKey transformation to get summary information about tiles overlapping an area

Sometimes you'd like to take a layer that has multiple tiles over the same spatial area through time,
and reduce it down to a layer that has only value per pixel, using some method of combining overlapping pixels.
For instance, you might want to find the maximum values of a pixel over time.

The following example shows an example of taking temperature data over time, and calculating the maximum temperature
per pixel for the layer:

```scala
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

val temperaturePerMonth: TileLayerRDD[SpaceTimeKey] = ???

val maximumTemperature: RDD[(SpatialKey, Tile)] =
  temperaturePerMonth
    .map { case (key, tile) =>
      // Get the spatial component of the SpaceTimeKey, which turns it into SpatialKey
      (key.getComponent[SpatialKey], tile)
    }
    // Now we have all the tiles that cover the same area with the same key.
    // Simply reduce by the key with a localMax
    .reduceByKey(_.localMax(_))
```

### Tiling an RDD of spatial tiles, stitching and saving off as a single GeoTiff

This example will show how to start with an `RDD[(ProjectedExtent, Tile)]` and end with a stitched together GeoTiff.


__Note__: Stitching together an RDD can produce a tile that is far bigger than the driver program's memory can handle.
You should only do this with small layers, or a filtered RDD.

```scala
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.vector._
import org.apache.spark.HashPartitioner
import org.apache.spark.rdd.RDD

val rdd: RDD[(ProjectedExtent, Tile)] = ???

// Tile this RDD to a grid layout. This will transform our raster data into a
// common grid format, and merge any overlapping data.

// We'll be tiling to a 512 x 512 tile size, and using the RDD's bounds as the tile bounds.
val layoutScheme = FloatingLayoutScheme(512)

// We gather the metadata that we will be targeting with the tiling here.
// The return also gives us a zoom level, which we ignore.
val (_: Int, metadata: TileLayerMetadata[SpatialKey]) =
  TileLayerMetadata.fromRdd(rdd, layoutScheme)

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

// Now we can save this layer off to a GeoTrellis backend (Accumulo, HDFS, S3, etc)
// In this example, though, we're going to just filter it by some bounding box
// and then save the result as a GeoTiff.

val areaOfInterest: Extent = ???

val raster: Raster[Tile] =
  layerRdd
    .filter()                            // Use the filter/query API to
    .where(Intersects(areaOfInterest))   // filter so that only tiles intersecting
    .result                              // the Extent are contained in the result
    .stitch                 // Stitch together this RDD into a Raster[Tile]

GeoTiff(raster, metadata.crs).write("/some/path/result.tif")
```

#### Applying a threshold and then median filter on multiband imagery in an RDD layer

This example shows how to take some multiband imagery that exists in a layer, filter it with some upper bound threshold,
and then apply a 5x5 median filter.

```scala
import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Square

val imageLayer: MultibandTileLayerRDD[SpaceTimeKey] = ???
val neighborhood = Square(2)

val resultLayer: MultibandTileLayerRDD[SpaceTimeKey] =
  imageLayer
    .withContext { rdd =>
      rdd.mapValues { tile =>
        tile.map { (band, z) =>
          if(z > 10000) NODATA
          else z
        }
      }
      .bufferTiles(neighborhood.extent)
      .mapValues { bufferedTile =>
        bufferedTile.tile.mapBands { case (_, band) =>
          band.focalMedian(neighborhood, Some(bufferedTile.targetArea))
        }
      }
    }
```

### Query region, mask by that region, compute max NDVI and save as a GeoTiff

```scala
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util._
import geotrellis.vector._
import org.joda.time.DateTime

val region: MultiPolygon = ???
val layerReader: FilteringLayerReader[LayerId] = ???
val layerId: LayerId = LayerId("layerName", 18) // Querying zoom 18 data

val queryResult: MultibandTileLayerRDD[SpaceTimeKey] =
  layerReader.query[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](layerId)
    .where(Intersects(region))
    .where(Between(new DateTime(2016, 3, 1, 0, 0, 0), new DateTime(2016, 4, 1, 0, 0)))
    .result

val raster: Raster[Tile] =
  queryResult
    .mask(region)
    .withContext { rdd =>
      rdd
        .mapValues { tile =>
          // Assume band band 4 is red and band 5 is NIR
          tile.convert(DoubleConstantNoDataCellType).combine(4, 5) { (r, nir) =>
            (nir - r) / (nir + r)
          }
        }
        .map { case (key, tile) => (key.getComponent[SpatialKey], tile) }
        .reduceByKey(_.localMax(_))
    }
    .stitch

GeoTiff(raster, queryResult.metadata.crs).write("/path/to/result.tif")
```
