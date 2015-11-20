## geotrellis.raster.render

#### ColorBreaks
ColorBreaks can be used to specify custom painting of cells when
geotrellis renders a raster.

The `fromStringInt` and `fromStringDouble` helper methods on
`ColorBreaks` allow you to deserialize a string of the form
"<limitString>:<hexColor>;<limitString>:<hexColor>" into an
IntColorBreak or a DoubleColorBreak, respectively.
