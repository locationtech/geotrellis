Raster Operations
-----------------
#### Local operations 

Local operations process each raster cell individually, without considering
other cells' values. When combining multiple rasters, a local operation will
map the value for each input's `(x, y)` cell to a single value in the output
raster's `(x, y)` cell. These properties allow GeoTrellis to parallelize local
operations very effectively.

Located in [geotrellis.raster.op.local.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.raster.op.local.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<tr><td><code>local.Add</code></td><td>Add the values of each cell in each raster.</td></tr>
<tr><td><code>local.And</code></td><td>Apply bitwise-and to each cell.</td></tr>
<tr><td><code>local.Ceil</code></td><td>Round each cell's value up to the nearest integer.</td></tr>
<tr><td><code>local.Defined</code></td><td>Set cell to 1 if defined, 0 if NoData.</td></tr>
<tr><td><code>local.Divide</code></td><td>Divide the values of each cell in each raster.</td></tr>
<tr><td><code>local.DoCell</code></td><td>Apply a custom function to each cell.</td></tr>
<tr><td><code>local.Equal</code></td><td>Set cell to 1 if equal to other cell, 0 if not.</td></tr>
<tr><td><code>local.Floor</code></td><td>Round each cell's value down to the nearest integer.</td></tr>
<tr><td><code>local.Greater</code></td><td>Set cell to 1 if cell is greater to other cell, 0 if not.</td></tr>
<tr><td><code>local.GreaterOrEqual</code></td><td>Set cell to 1 if cell is greater than or equal to other cell, 0 if not.</td></tr>
<tr><td><code>local.IfCell</code></td><td>Given a condition, set cell to value if true.</td></tr>
<tr><td><code>local.IfElseCell</code></td><td>Given a condition, set cell to true or false value.</td></tr>
<tr><td><code>local.Less</code></td><td>Set cell to 1 if cell is less to other cell, 0 if not.</td></tr>
<tr><td><code>local.LessOrEqual</code></td><td>Set cell to 1 if cell is less than or equal to other cell, 0 if not.</td></tr>
<tr><td><code>local.Log</code></td><td>Take the logarithm (base e) of each cell.</td></tr>
<tr><td><code>local.Mask</code></td><td>Set cells in raster to NoData based on values in other raster.</td></tr>
<tr><td><code>local.Max</code></td><td>Set cells to the maximum value.</td></tr>
<tr><td><code>local.Min</code></td><td>Set cells to the minimum value.</td></tr>
<tr><td><code>local.Multiply</code></td><td>Multiply the values of each cell in each raster.</td></tr>
<tr><td><code>local.Negate</code></td><td>Multiply cell values by -1.</td></tr>
<tr><td><code>local.Not</code></td><td>Apply bitwise-not to each cell.</td></tr>
<tr><td><code>local.Or</code></td><td>Apply bitwise-or to each cell.</td></tr>
<tr><td><code>local.Pow</code></td><td>Raise each cell to the specified power.</td></tr>
<tr><td><code>local.Round</code></td><td>Round each cell's value to the nearest integer.</td></tr>
<tr><td><code>local.Sqrt</code></td><td>Take the square root of each cell.</td></tr>
<tr><td><code>local.Subtract</code></td><td>Subtract the values of each cell in each raster.</td></tr>
<tr><td><code>local.Undefined</code></td><td>Set cell to 0 if defined, 1 if NoData.</td></tr>
<tr><td><code>local.Unequal</code></td><td>Set cell to 1 if not equal to other cell, 0 if not.</td></tr>
<tr><td><code>local.Xor</code></td><td>Apply bitwise-xor to each cell.</td></tr>
</tbody>
</table>

#### Focal operations
Raster operations that work on raster cells and their neighbors. Located in [geotrellis.raster.op.focal.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.raster.op.focal.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<<<<<<< HEAD
<tr><td><code>focal.Rescale</code></td><td>Rescale values between new min and max value.</td></tr>
<tr><td><code>focal.Hillshade</code></td><td>Create a three dimensional appearance from an elevation raster.</td></tr>
<tr><td><code>focal.KernelDensity</code></td><td>Compute the kernel density of a set of points onto a raster.</td></tr>
<tr><td><code>focal.MoransI</code></td><td>Compute the Morans I of the input raster.</td></tr>
<tr><td><code>focal.LocalMoransI</code></td><td>Set all cells to Local Morans I.</td></tr>
=======
>>>>>>> non/topic/doc-updates
<tr><td><code><i>focal.Aspect</i></code></td><td>Calculate downslope direction from each cell to its neighbors.</td></tr>
<tr><td><code><i>focal.CostDistance</i></code></td><td>Calculate cost distance raster.</td></tr>
<tr><td><code><i>focal.FlowDirection</i></code></td><td>Calculate flow direction from input raster.</td></tr>
<tr><td><code>focal.Hillshade</code></td><td>Create a three dimensional appearance from an elevation raster.</td></tr>
<tr><td><code>focal.KernelDensity</code></td><td>Compute the kernel density of a set of points onto a raster.</td></tr>
<tr><td><code><i>focal.Max</i></code></td><td>Set pixels to maximum value in neighborhood.</td></tr>
<tr><td><code><i>focal.Mean</i></code></td><td>Set pixels to mean of specified neighborhood.</td></tr>
<tr><td><code><i>focal.Min</i></code></td><td>Set pixels to minimum value in neighborhood.</td></tr>
<tr><td><code>focal.Rescale</code></td><td>Rescale values between new min and max value.</td></tr>
<tr><td><code><i>focal.Slope</i></code></td><td>Calculate maximum rate of change cell to its neighbors.</td></tr>
<tr><td><code><i>focal.StandardDeviation</i></code></td><td>Set each cell to standard deviation within the specified neighborhood.</td></tr>
<tr><td><code><i>focal.Viewshed</i></code></td><td>Calculate viewshed from input points.</td></tr>
</tbody>
</table>


#### Zonal operations
Raster operations that work on areas of cells that share the same value in an input raster. Located in [geotrellis.raster.op.zonal.](http://azavea.github.com/geotrellis/latest/api/#geotrellis.raster.op.zonal.package)

<table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
<<<<<<< HEAD
<tr><td><code>zonal.StandardDeviation</code></td><td>Set each cell to standard deviation within zone.</td></tr>
<tr><td><code><i>zonal.Mean</i></code></td><td>Set pixels to mean of their zone.</td></tr>
<tr><td><code><i>zonal.Max</i></code></td><td>Set pixels to maximum value in their zone.</td></tr>
<tr><td><code><i>zonal.Min</i></code></td><td>Set pixels to minimum value in their zone.</td></tr>
=======
<tr><td><code><i>zonal.Max</i></code></td><td>Set pixels to maximum value in their zone.</td></tr>
<tr><td><code><i>zonal.Mean</i></code></td><td>Set pixels to mean of their zone.</td></tr>
<tr><td><code><i>zonal.Min</i></code></td><td>Set pixels to minimum value in their zone.</td></tr>
<tr><td><code><i>zonal.StandardDeviation</i></code></td><td>Set each cell to standard deviation within zone.</td></tr>
>>>>>>> non/topic/doc-updates
</tbody>
</table>

<i>(Italicized operations are not yet implemented.)</i>
