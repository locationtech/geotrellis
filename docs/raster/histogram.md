# Histogram

It's often useful to derive a histogram from rasters, which represents a distribution of the values of the raster in a more compact form.
In GeoTrellis, we differentiate between a `Histogram[Int]`, which represents the exact counts of integer values, and a `Histogram[Double]`,
which represents a grouping of values into a discrete number of buckets. These types are in the `geotrellis.raster.histogram` package.

The default implementation of `Histogram[Int]` is the `FastMapHistogram`, developed by Erik Osheim, which utilizes an growable array structure
for keeping track of the values and counts.

The default implementation of `Histogram[Double]` is the `StreamingHistogram`, developed by James McClain and based on the paper `Ben-Haim, Yael, and Elad Tom-Tov. "A streaming parallel decision tree algorithm."  The Journal of Machine Learning Research 11 (2010): 849-872.`.

Histograms can give statistics such as min, max, median, mode and median.
It also can derive quantile breaks, as dsecribed in the next section.

## Quantile Breaks

Dividing a histogram distribution into quantile breaks attempts to classify values into some number of buckets,
where the number of classified into each bucket are generally equal.
This can be useful in representing the distribution of the values of a raster.

For instance, say we had a tile with mostly values between 1 and 100, but there were a few values that were 255.
We want to color the raster with 3 values: low values with red, middle values with green, and high values with blue.
In other words, we want to classify each of the raster values into one of three categories: red, green and blue.
One technique, called equal interval classification, consists of splitting up the range of values (1 - 255) into
the number of equal intervals as target classifications (3). This would give us a range intervals of 1 - 85 for red,
86 - 170 for green, and 171 - 255 for blue. This corresponds to "breaks" values of 85, 170, and 255.
Because the values are mostly between 1 and 100, most of our raster would
be colored red. This may not show the contrast of the dataset that we would like.

Another technique for doing this is called quantile break classification; this makes use of the quantile breaks we can
derive from our histogram. The quantile breaks will concentrate on making the number of values per "bin" equal, instead
of the range of the interval. With this method, we might end up seeing breaks more like 15, 75, 255, depending on the distribution
of the values.

For a code example, this is how we would do exactly what we talked about: color a raster tile into red, green and blue values based
on it's quantile breaks:

```scala
import geotrellis.raster.histogram._
import geotrellis.raster.render._

val tile: Tile = ???  // Some raster tile
val histogram: Histogram[Int] = tile.histogram

val colorRamp: ColorRamp =
  ColorRamp(
    RGB(r=0xFF, b=0x00, g=0x00),
    RGB(r=0x00, b=0xFF, g=0x00),
    RGB(r=0x00, b=0x00, g=0xFF)
  )

val colorMap = ColorMap.fromQuantileBreaks(histogram, colorRamp)

val coloredTile: Tile = tile.color(colorMap)
```
