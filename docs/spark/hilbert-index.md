This document is a gathering-place for useful information about the
Hilbert Curve Index implementation in Geotrellis and the `uzaygezen`
library on which it is based.

### The Index Resolution Changes Index Order ###

This is a well-known fact, but there is no harm in a reminder.

Changing the resolution (in bits) of the index causes a rotation
and/or reflection of the points with respect to curve-order. An
example is in the test file `HilbertGridTimeKeyIndexSpec.scala`
where the line

```scala
HilbertGridTimeKeyIndex(GridTimeKey(0,0,y2k), GridTimeKey(2,2,y2k.plusMillis(1)),2,1)
```

appears (with the last two arguments being resolutions).  If that is changed to

```scala
HilbertGridTimeKeyIndex(GridTimeKey(0,0,y2k), GridTimeKey(2,2,y2k.plusMillis(1)),3,1)
```

then the index-order of the points will be different.

### 62-bit Limit ###

Currently, the spatial and temporal resolution required to index the points,
expressed in bits, must sum to 62 bits or fewer.

For example, the following code appears in `HilbertSpacetimeKeyIndex.scala`:

```scala
@transient lazy val chc = {
  val dimensionSpec =
    new MultiDimensionalSpec(
      List(
        xResolution,
        yResolution,
        temporalResolution
      ).map(new java.lang.Integer(_))
    )
```

where `xResolution`, `yResolution` and `temporalResolution` are
numbers of bits required to express possible locations in each of
those dimensions.  If those three integers sum to more than 62 bits,
an error will be thrown at runtime.

