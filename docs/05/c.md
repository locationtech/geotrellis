Other Operations
----------------
#### Statistics operations

Operations for computing statistics from geographic and non-geographic data. [Scala package geotrellis.statistics.op.stat.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.statistics.op.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<tr><td><code>stat.GetHistogram</code></td><td>Returns a histogram of input values.</td></tr>
<tr><td><code>stat.GetMinMax</code></td><td>Returns minimum and maximum values.</td></tr>
<tr><td><code>stat.GetStatistics</code></td><td>Returns mean, mode, stddev, min and max.</td></tr>
<tr><td><code>stat.GetPolygonalZonalHistograms</code></td><td>Return a histogram of raster values within each zone.</td></tr>
<tr><td><code>stat.GetStandardDeviation</code></td><td>Returns standard deviation of raster values.</td></tr>
<tr><td><code><i>stat.GetVariance</i></code></td><td>Returns variance of raster values.</td></tr>
<tr><td><code>stat.GetColorBreaks</code></td><td>Returns colors for representation of raster value ranges.</td></tr>
<tr><td><code>stat.GetClassBreaks</code></td><td>Returns quantile class breaks for a given raster.</td></tr>
<tr><td><code>stat.GetColorsFromPalette</code></td><td>Perform an operation on each element in a sequence.</td></tr>
</tbody>
</table>

#### REST Operations

These operations are for use in construction of REST-style web services -- the current operations are all focused on string processing.  [Scala package geotrellis.rest.op.string.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.rest.op.string.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<tr><td><code>string.Concat</code></td><td>Concatenate strings.</td></tr>
<tr><td><code>string.ParseDouble</code></td><td>Parse a string as a double.</td></tr>
<tr><td><code>string.ParseExtent</code></td><td>Parse a bounding box string as an extent.</td></tr>
<tr><td><code>string.ParseInt</code></td><td>Parse a string as an integer.</td></tr>
<tr><td><code>string.ParseHexInt</code></td><td>Parse a string as a hexidecimal integer.</td></tr>
<tr><td><code>string.Split</code></td><td>Split a string on a delimeter.</td></tr>
</tbody>
</table>


#### Vector Operations (0.7)

Vector operations operate over features such as polygons, lines, and points.  GeoTrellis vector feature
support is scheduled for a major overhaul and expansion for release 0.8, and you should consider consulting with the development team and tracking the GeoTrellis development version if you are planning on relying on existing functionality and API.  A few sample vector operations are listed below.

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<tr><td><code>data.RasterizePolygon</code></td><td>Create raster representation of vector data.</td></tr>
<tr><td><code>data.SplitMultiPolygon</code></td><td>Split multipolygon in to individual polygons.</td></tr>
<tr><td><code>geometry.Intersect</code></td><td>Return the intersection of input geometries.</td></tr>
<tr><td><code>extent.PolygonExtent</code></td><td>Return the bounding box of the input polygon.</td></tr>
</tbody>
</table>


