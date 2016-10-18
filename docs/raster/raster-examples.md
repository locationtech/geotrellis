# Examples

### Matching two rasters of a different CRS so that you can perform operations between them.

Consider you have two rasters, `r1` and `r2`, that are of a different resolution and CRS.
Say we wanted to take a weighted sum between them over some Extent `e`.
In order to do map algebra between the two rasters, they must match extends and resolution.
We can do this by picking one of the raster's CRS and resolution, and modifying the other
to match it. Here is an example of that:
