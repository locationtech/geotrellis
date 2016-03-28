# geotrellis.raster.mapalgebra

The contents of `mapalgebra` are organized by type of operation. This
is a brief introduction to those submodules through the lens of those
operation types. Some of these types can be understood as separating
concerns about how location-aware their operations need to be; keep
an eye out for that.

- [`geotrellis.raster.local`](../../raster/src/main/scala/geotrellis/raster/mapalgebra/local)
defines operations which act on a cell without regard to its spatial
relations. Need to
double every cell on a tile? This is the module you'll want to explore.
- [`geotrellis.raster.focal`](../../raster/src/main/scala/geotrellis/raster/mapalgebra/focal)
defines operations operations which focus on two-dimensional windows
(internally referred to as neighborhoods) of a raster's values to
determine their outputs.
[Conway's Game of Life](http://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)
can be seen as a focal operation in that each cell's value depends on
neighboring cell values. Though focal operations will tend to look at
a local region of this or that cell, they should not be confused with
the operations which live in `geotrellis.raster.local` - those
operations describe transformations over tiles which, for any step of
the calculation, need only know the input value of the specific cell
for which it is calculating an output (e.g. incrementing each cell's
value by 1).
- [`geotrellis.raster.zonal`](../..raster/src/main/scala/geotrellis/raster/mapalgebra/zonal)
defines operations which apply over a zones as defined by corresponding
cell values in the zones raster.
