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

####Rendering your first tile
Let's say that the tile you've got is an integer tile and that the
integers in it are all equivalent to hex colors


####ColorBreaks
ColorBreaks can be used to specify custom painting of cells when
geotrellis renders a raster.

The `fromStringInt` and `fromStringDouble` helper methods on
`ColorBreaks` allow you to deserialize a string of the form
"<limitString>:<hexColor>;<limitString>:<hexColor>" into an
IntColorBreak or a DoubleColorBreak, respectively.
