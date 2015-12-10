#geotrellis.raster.render

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

####First Steps
Let's say that the tile you've got is an integer tile and that the
integers in it are all *actually* hex codes for RGB colors. In this
simple case, your task is nearly complete.

```scala
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.render.png._

// Generate the tile - let's paint it red with #FF0000
// (red = 0xFF or 255; green = 0x00 or 0; blue = 0x00 or 0)
val hexColorsHere: IntArrayTile = IntArrayTile.fill(0xFF0000, 100, 100)

// Making the PNG
val pngData: Png = hexColorsHere.renderPng
// alternatively...
// geotrellis.raster.render.jpg._
// hexColorsHere.renderJpg
```

Let's take a look at the types of Png and Jpg to get a sense
 of how they operate. You'll find further detail in ImageFormats.scala
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
tile relate to the colors you'd like in your image. To this end, you can
provide a 


####ColorBreaks
ColorBreaks can be used to specify custom painting of cells when
geotrellis renders a raster.

The `fromStringInt` and `fromStringDouble` helper methods on
`ColorBreaks` allow you to deserialize a string of the form
"<limitString>:<hexColor>;<limitString>:<hexColor>" into an
IntColorBreak or a DoubleColorBreak, respectively.
