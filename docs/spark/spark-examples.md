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
