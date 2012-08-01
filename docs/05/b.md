Logic and IO Operations
-----------------------

These sets of operation are relevant to all compound operations as they are used to load data, produce output formats, and as the functional programming constructs that are critical to operation composition.

#### IO operations 

IO operations provide input, loading data, and output, translating the output of geoprocessing models
into output formats.  [Scala package geotrellis.io.op.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.io.op.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<tr><td><code><i>io.LoadFeatures</i></code></td><td>Load vector features from catalog.</td></tr>
<tr><td><code>io.LoadFile</code></td><td>Load a raster from the provide file path.</td></tr>
<tr><td><code>io.LoadRaster</code></td><td>Load a raster by name from the catalog.</td></tr>
<tr><td><code>io.LoadRasterExtent</code></td><td>Load the raster extent by name from the catalog.</td></tr>
<tr><td><code>io.LoadRasterExtentFromFile</code></td><td>Load the raster extent by file path.</td></tr>
<tr><td><code>io.ReclassifyColorsRgba</code></td><td>Reclassify data to output color values.</td></tr>
<tr><td><code>io.RenderPNG</code></td><td>Generate a PNG from a given raster and a set of color breaks.</td></tr>
<tr><td><code>io.WritePNGFile</code></td><td>Write a PNG to disk.</td></tr>
</tbody>
</table>

#### Logic operations

Logical operations for composing compound operations.  [Scala package geotrellis.logic.op.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.logic.op.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<tr><td><code>logic.Collect</code></td><td>Maps a sequence of operations into an operation that returns a sequence.</td></tr>
<tr><td><code>logic.Do</code></td><td>Perform a function on the input, like 'map' in functional programing.</td></tr>
<tr><td><code>logic.ForEach</code></td><td>Perform an operation on each element in a sequence.</td></tr>
</tbody>
</table>

For Haskell-inspired syntax, see [see geotrellis.logic.applicative](http://azavea.github.com/geotrellis/latest/api/#geotrellis.logic.applicative.package) package.
