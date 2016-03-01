# geotrellis.raster.render

## Rendering Common Image Formats
At some point, you'll want to output a visual representation of the
tiles you're processing. Likely, that's why you're reading this bit of
documentation. Luckily enough, geotrellis provides some methods which
make the process as painless as possible. Currently, both PNG and JPG
formats are supported.

To begin writing your tiles out as PNGs and JPGs, there are just a
few things to keep in mind. As elsewhere throughout geotrellis, the
functionality in this module is added through implicit class extension.
Remember to `import geotrellis.raster.render._` before you try to use
the methods you find here.

#### First Steps
Let's say that the tile you've got is an integer tile and that the
integers in it are all *actually* hex codes for RGB colors. In this
case, your task is nearly complete. The following code should be
sufficient:

```scala
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.render.png._

// Generate the tile - let's paint it red with #FF0000
// (red = 0xFF or 255; green = 0x00 or 0; blue = 0x00 or 0)
val hexColored: IntArrayTile = IntArrayTile.fill(0xFF0000, 100, 100)

// Making a PNG
val pngData: Png = hexColored.renderPng

// JPG variation
// geotrellis.raster.render.jpg._
// hexColorsHere.renderJpg
```

Let's take a look at the types of Png and Jpg to get a sense
 of how they operate. You'll find further detail in
[ImageFormats.scala](../../raster/src/main/scala/geotrellis/raster/render/ImageFormats.scala)

```scala
// constructor defintions
case class Png(bytes: Array[Byte]) extends ImageFormat
case class Jpg(bytes: Array[Byte]) extends ImageFormat

// their only method
def write(f: File): Unit
```

The array of bytes created from calling renderPng is wrapped
with a `write` method that provides a clean interface for writing
those bytes to a file.

Clearly this won't suffice for the majority of use-cases. In general,
you're more likely to be working on tiles whose cells encode information
having only an incidental relation to human vision. In these cases,
you'll need to tell `renderPng` and `renderJpg` how the values in your
tile relate to the colors you'd like in your image. To this end, there
are arguments you can provide to the render method which
will tell geotrellis how to color cells for your tile.


## Color Classification
When your raster encodes infrared or elevation data, color isn't as
straightforward as the example above. ColorClassifiers are used to
specify, how to paint cells when geotrellis renders a raster of
values that aren't simply integer representations of color. You can look
into the source
[here](../../raster/src/main/scala/geotrellis/raster/render/color/ColorClassifier.scala).

The basic idea is to generate a color scheme by specifying boundaries on
said data. A piece of data can be said to be classified in terms of its
color. There are a few different workflows for specifying a raster's
color classes. We'll discuss three:

### Strict Classification
In some ways the simplest form of classification, strict color
classification involves explicitly specifying the boundary for each
class and its associated color.

```scala
// A couple ways to go about this

// Constructing a classifier, piecemeal:
val constructingClassifier = new StrictIntColorClassifier
// Every value below 100 should be red
constructingClassifier.classify(100, RGBA(255, 0, 0, 255))

// Generating a strict classifier with companion object methods
val intBasedClassifier = StrictColorClassifier(Array[(Int, RGBA)]((100,
RGBA(255, 0, 0, 255)), (200, RGBA(0x00FF0000)))

val doubleBasedClassifier = StrictColorClassifier(Array[(Double, RGBA)]((100.0,
RGBA(255, 0, 0, 255)), (200.12, RGBA(0x00FF0000)))
```

### Blending Classification
Blending color classification is useful in cases where a gradual ramp of
colors would be preferable to explicitly stating data-to-color
relations. Prior to rendering, these classifiers call a method
`normalize` which generates colors to match its breaks.

```scala
// Given 2 breaks and 5 colors, interpolate 2 colors to match the breaks
val fiveColors = Array[Int](1, 100, 1000, 2000, 50000).map(RGBA(_))
val twoBreaks = Array[Int](100, 30000)
val colorReducer = new BlendingIntColorClassifier
  .addColors(fiveColors)
  .addBreaks(twoBreaks)

// Given 5 breaks and two colors, interpolate 5 colors to match the breaks
val twoColors = Array[Int](1, 10000).map(RGBA(_))
val fiveBreaks = Array[Int](1, 2000, 32132, 12354, 42)
val colorIncreaser = new BlendingIntColorClassifier
  .addColors(twoColors)
  .addBreaks(fiveBreaks)
```

All you need to use blending color classification is to ensure that two
colors and at least one break are supplied prior to rendering your tile.


### Quantile Generated Classification
Technically, this is a special case of strict classification, but the
workflow is slightly different. If you have a set of colors that you'd
like to be dispersed evenly, that list of colors along with a histogram
are up to the task.

```scala
// This should generate 5 breaks for our colors
val histogram: Histogram[Int] = ???
val colors = Array[Int](1, 100, 1000, 2000, 50000).map(RGBA(_))
val genClassifier = StrictColorClassifier.fromQuantileBreaks(histogram, colors)
```

## Render Settings
It might be useful to tweak the rendering of images for some use cases.
In light of this fact, both png and jpg expose a `Settings` classes
(`geotrellis.raster.render.jpg.Settings` and
`geotrellis.raster.render.png.Settings`) which provide a means to tune
image encoding.
In general, messing with this just isn't necessary. If you're unsure,
there's a good chance this featureset isn't for you.

#### PNG Settings
`png.Settings` allows you to specify a `ColorType` (bit depth and masks)
and a `Filter`. These can both be read about on the W3 specification and
[png Wikipedia
page]('https://en.wikipedia.org/wiki/Portable_Network_Graphics').

#### JPEG Settings
`jpg.Settings` allow specification of the compressionQuality (a Double
from 0 to 1.0) and whether or not Huffman tables are to be computed on
each run - often referred to as 'optimized' rendering. By default, a
compressionQuality of 0.7 is used and Huffman table optimization is not used.
