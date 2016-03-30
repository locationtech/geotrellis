# Hilbert index

GeoTrellis uses the `uzaygezen` library's Hilbert curve implementation.

### Index Resolution Changes Index Order

Changing the resolution (in bits) of the index causes a rotation
and/or reflection of the points with respect to curve-order.
Take, for example the following code (which is actually derived from the
testing codebase):

```scala
HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(1)),2,1)
```

The last two arguments are the index resolutions. If that were changed to:

```scala
HilbertSpaceTimeKeyIndex(SpaceTimeKey(0,0,y2k), SpaceTimeKey(2,2,y2k.plusMillis(1)),3,1)
```

The index-order of the points would be different. The reasons behind
this are ultimately technical, though you can imagine how a naive
implementation of an index for, say, a 10x10 matrix (in terms of 100
numbers) would need to be reworked if you were to change the number of
cells (100 would no longer be enough for an 11x11 matrix and the pattern
for indexing you chose may no longer make sense). Obviously, this is
complex and beyond the scope of GeoTrellis' concerns, which is why we
lean on Google's `uzaygezen` library.

### Beware the 62-bit Limit

Currently, the spatial and temporal resolution required to index the points,
expressed in bits, must sum to 62 bits or fewer.

For example, the following code appears in `HilbertSpaceTimeKeyIndex.scala`:

```scala
@transient
lazy val chc = {
  val dimensionSpec =
    new MultiDimensionalSpec(
      List(
        xResolution,
        yResolution,
        temporalResolution
      ).map(new java.lang.Integer(_))
    )
}
```

where `xResolution`, `yResolution` and `temporalResolution` are
numbers of bits required to express possible locations in each of
those dimensions.  If those three integers sum to more than 62 bits,
an error will be thrown at runtime.

