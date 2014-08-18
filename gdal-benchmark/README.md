GDAL Benchmarking
=================

This project is for benchmarking against GDAL. Right now it's a place to benchmark the GDAL GeoTiff reader, our native GeoTiff reader, and the GeoTools GeoTiff reader against each other.

Last updated stats (all run on a dev machine, relative numbers, beware of microbenchmarks, yada yada):

#### Large Uncompressed

```
[info]             benchmark    ms linear runtime
[info]     GDALReadAspectTif  43.8 ====
[info]   NativeReadAspectTif  45.6 ====
[info] GeotoolsReadAspectTif 290.8 ==============================
```

#### CCITTFAX3 Compression

```
[info]             benchmark    ms linear runtime
[info]     GDALReadCCITTFAX3  4.59 =====
[info]   NativeReadCCITTFAX3  6.81 ========
[info] GeotoolsReadCCITTFAX3 24.41 ==============================
```

#### CCITTFAX4 Compression

```
[info]             benchmark    ms linear runtime
[info]     GDALReadCCITTFAX4  4.66 =====
[info]   NativeReadCCITTFAX4  6.64 =======
[info] GeotoolsReadCCITTFAX4 26.28 ==============================

```

#### Uncompressed

```
[info]     GDALUncompressed  4.69 =====
[info]   NativeUncompressed  6.66 ========
[info] GeotoolsUncompressed 24.53 ==============================
```

#### Tiled Uncompressed

```
[info]                 benchmark    ms linear runtime
[info]     GDALTiledUncompressed  6.26 =====
[info]   NativeTiledUncompressed 13.15 ===========
[info] GeotoolsTiledUncompressed 32.91 ==============================
```

#### LZW Compression

```
[info]   benchmark    ms linear runtime
[info]     GDALLZW  3.27 ==
[info]   NativeLZW 35.42 ==============================
[info] GeotoolsLZW 32.39 ===========================
```

#### Packed Bits Compression

```
[info]          benchmark    us linear runtime
[info]     GDALPackedBits   798 ==
[info]   NativePackedBits  1052 ==
[info] GeotoolsPackedBits 10651 ==============================
```

#### ZLib Compression

```
[info]    benchmark    us linear runtime
[info]     GDALZLib   818 =
[info]   NativeZLib  1057 ==
[info] GeotoolsZLib 12322 ==============================
```

#### Uncompressed ESRI GeoTiff

```
[info]                benchmark   us linear runtime
[info]     GDALUncompressedESRI  261 ==
[info]   NativeUncompressedESRI  165 =
[info] GeotoolsUncompressedESRI 2649 ==============================
```

#### Uncompressed ESRI Stripped GeoTiff

```
[info]                        benchmark     us linear runtime
[info]     GDALUncompressedESRIStripped  305.7 ===
[info]   NativeUncompressedESRIStripped   77.8 =
[info] GeotoolsUncompressedESRIStripped 2832.5 ==============================
```
