#geotrellis.raster.resample

##Resampling
Often, when working with raster data, it is useful to change the number
of rows and/or columns. For everything there is a price, however, and
changing the resolution of a tile is no exception: there will (almost)
always be a loss of information (or representativeness) when conducting
an operation which changes the number of cells on a tile.  

####Upsampling vs Downsampling
Intuitively, there are two ways that you might resample a tile. You
might:  
1. increase the number of cells  
2. decrease the number of cells

Increasing the number of cells produces more information at the cost of
being only probabilistically representative of the underlying data whose
points are being used to generate new values. We can call this
upsampling (because we're increasing the samples for a given
representation of this or that state of affairs). Typically, upsampling
is handled through interpolation of new values based on the old ones.  

The opposite, downsampling, involves a loss of information. Fewer points of
data are tasked with representing the same states of affair as the tile on
which the downsampling is carried out. Downsampling is a common strategy
in digital compression.

####Aggregate vs Point-Based Resampling
In geotrellis, `ResampleMethod` is an ADT (through a sealed trait
in `Resample.scala`) which branches into `PointResampleMethod` and
`AggregateResampleMethod`. The aggregate methods of resampling are all
suited for downsampling only. For every extra cell created by upsampling with
an `AggregateResampleMethod`, the resulting tile is *absolutely certain*
to contain a `NODATA` cell. This is because for each additional cell produced
in an aggregated resampling of a tile, a bounding box is generated which
determines the output cell's value on the basis of an aggregate of the
data captured within said bounding box. The more cells produced through
resampling, the smaller an aggregate bounding box. The more cells
produced through resampling, the less likely it is that this box will
capture any values to aggregate over.  

What we call 'point' resampling doesn't necessarily require a box within
which data is aggregated. Rather, a point is specified for which a value
is calculated on the basis of nearby value(s). Those nearby values may
or may not be weighted by their distance from the point in question.
These methods are suitable for both upsampling and downsampling.  

####Remember What Your Data Represents
Along with the formal characteristics of these methods, it is important
to keep in mind the specific character of the data that you're working
with. After all, it doesn't make sense to use a method like
`Bilinear` resampling if you're dealing primarily with categorical data.
In this instance, your best bet is to choose an aggregate method (and
keep in mind that the values generated don't necessarily mean the same
thing as the data being operated on) or a forgiving (though
unsophisticated) method like `NearestNeighbor`.
