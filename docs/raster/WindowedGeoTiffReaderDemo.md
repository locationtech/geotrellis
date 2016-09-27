## Demo for the WindowedGeoTiffReader

- [Using this Demo](#using-this-demo)
- [Getting Started](#getting-started)
- [Small Files Benchmark](#small-files-benchmark)
  - [Running the Small Files Benchmark](#running-the-small-files-benchmark)
  - [Understanding the Output: Small Files](#understanding-the-output-small-files)
  - [Interpreting the Results: Small Files](#interpreting-the-results-small-files)
- [Large Files Benchmark](#large-files-benchmark)
  - [Running the Large Files Benchmark](#running-the-large-files-benchmark)
  - [Understanding the Output: Large Files](#understanding-the-output-large-files)
  - [Interpreting the Results: Large Files](#interpreting-the-results-large-files)

### Using this Demo

**Note:** Before starting this demo, it is advised that you read through the docs about the [WindowedGeoTiffReader](./WindowedGeoTiffReader.md) feature first.

In this demo, we will use the new WindowedGeoTiffReader to see how it runs at various extents for the Byte and Float64 bandtypes. It will be compared against the traditional way of reading in a file, which is reading in the entire thing and then cropping it, to see the performance of each type. To perform these benchmarks, the [Geotrellis benchmark](https://github.com/geotrellis/geotrellis-benchmark) tool was used.
- - -

### Getting Started

To start, go to the into the data directory. There, you will see a BASH script file called unzip_geotiffs.sh. In the terminal, type the following command:

```console
> chmod +x ./unzip_geotiffs.sh
```
This allows the shell script to be executed from within your terminal.
Next, enter:

```console
> ./unzip_geotiffs
```

Now the script will execute, and it will create two folders in the data directory: small-files and large-files. Within both of these folders are two GeoTiff files: byte.tif and float64.tif. These represent geotiffs that contain byte and float64 bandtypes, respectivily.
- - -

### Small Files Benchmark

The first benchmark that will be run in this demo is for small files. What constitutes a "small" file is a GeoTiff that can be read in fully without causig the process to crash due to an out of memory error. This is an important distinction as the methods needed to obtain the best results for reading in these types of files is different from those trying to read in a file that is too large to loaded in fully.

#### Running the Small Files Benchmarks

With the files that are needed to run the benchmark unzipped, we can now begin. Go back up to the root project folder of the demo, and then copy the following command into the terminal:

```console
> ./sbt "project windowedreader-benchmark" "test-only *SmallFilesBenchmark*"
```

There will be a lot of output, the two key areas are these:

```console
Running benchmarks for Reading in small Extents of small files...
  Name                                    ms  linear runtime
  5%, WGR, Byte                         1.53  ==
  5%, cropping, Byte                    1.97  ==

  5%, WGR, Float64                      2.57  ===
  5%, cropping, Float64                13.68  =================

  10%, WGR, Byte                        1.56  ==
  10%, cropping, Byte                   1.89  ==

  10%, WGR, Float64                     2.93  ===
  10%, cropping, Float64               16.23  =====================

  15%, WGR, Byte                        1.31  =
  15%, cropping, Byte                   2.17  ==

  15%, WGR, Float64                     3.34  ====
  15%, cropping, Float64               13.76  ==================

Running benchmarks for Reading in large Extents of small files...

  Name                                    ms  linear runtime
  90%, WGR, Byte                        1.90  ==
  90%, cropping, Byte                   2.11  ==

  90%, WGR, Float64                    11.22  =============
  90%, cropping, Float64               15.64  ===================

  95%, WGR, Byte                        2.05  ==
  95%, cropping, Byte                   2.11  ==

  95%, WGR, Float64                    11.82  ==============
  95%, cropping, Float64               15.70  ===================

  100%, WGR, Byte                       1.69  ==
  100%, cropping, Byte                  1.95  ==

  100%, WGR, Float64                   12.98  ================
  100%, cropping, Float64              15.33  ==================
```

#### Understanding the Output: Small Files

This benchmark tests the speed of both the new GeoTiffReader and the traditional way of reading and cropping a file on both Byte and Float64 bandtypes at various extents. Let's look at the first section under, "Running benchmarks for Reading in small extents for small files".

```console
Running benchmarks for Reading in small Extents of small files...

  Name                                    ms  linear runtime
  5%, WGR, Byte                         1.53  ==
  5%, cropping, Byte                    1.97  ==

  5%, WGR, Float64                      2.57  ===
  5%, cropping, Float64                13.68  =================

  10%, WGR, Byte                        1.56  ==
  10%, cropping, Byte                   1.89  ==

  10%, WGR, Float64                     2.93  ===
  10%, cropping, Float64               16.23  =====================

  15%, WGR, Byte                        1.31  =
  15%, cropping, Byte                   2.17  ==

  15%, WGR, Float64                     3.34  ====
  15%, cropping, Float64               13.76  ==================
```

On the left hand side, under the "Name" column, are the descriptions of each benchmark. The percentage is what percent of the original file that was read. Following the number, is what method was used for reading. "WGR" stands for WindowedGeoTiffReader, and cropping refers to reading in the whole file and then performing a crop on it. After the last comma, is the bandtype of the file for that specific test.

In the "ms" column to the right, is the time it took to run the test in miliseconds; and to the right of the number, is a visual representation of its linear run time.

The next section reads small files at larger extents. Its output is:

```console
Running benchmarks for Reading in large Extents of small files...

  Name                                    ms  linear runtime
  90%, WGR, Byte                        1.90  ==
  90%, cropping, Byte                   2.11  ==

  90%, WGR, Float64                    11.22  =============
  90%, cropping, Float64               15.64  ===================

  95%, WGR, Byte                        2.05  ==
  95%, cropping, Byte                   2.11  ==

  95%, WGR, Float64                    11.82  ==============
  95%, cropping, Float64               15.70  ===================

  100%, WGR, Byte                       1.69  ==
  100%, cropping, Byte                  1.95  ==

  100%, WGR, Float64                   12.98  ================
  100%, cropping, Float64              15.33  ==================
```

All the parameters for this benchmark are the same, except that now the areas being read in are larger than the first.

#### Interpreting the Results: Small Files

From the output, we can see that the WindowedGeoTiffReader out performs reading in the whole file and then cropping in every case. Sometimes the difference can be quite substational as can be seen in the smaller extents. Though, as the area to be read in gets larger, this increase becomes less. In some cases, it gets to the point where it is at the same level as the traditonal reader. Therefore, it is suggested that when reading these smaller files that one limits their extent to less than 90%; as this will ensure that the time taken to load the file will be faster than the traditional mehtod.

Even when using the WindowedGeoTiffReader there exists differences in time between in the bandtypes. Float64 is faster than Byte, and indeed, is faster than all other bandtypes as well. In general, the larger the bandtype, the more performance gain the user will have when utilizing the WindowedGeoTiffReader.

- - -

### Large Files Benchmark

In the second benchmark, larger files will be tested with the WindowedGeoTiffReader. Unlike the smaller files, these GeoTiffs cannot be read in fully without crashing the process. Also, as we will see, the results of windowed reading at these sized are different from their smaller counterparts.

#### Running the Large Files Benchmark

To run the benchmarks on the large files enter the following ito the terminal:

```console
> ./sbt "project windowedreader-benchmark" "test-only *LargeFilesBenchmarck*"
```

As with the small files benchmark, the important output is:

```console
Running benchmarks for Reading in small Extents of large files...

  Name                                     s  linear runtime
  5%, WGR, Byte                         0.29  =
  5%, WGR, Float64                      2.50  =========
  
  10%, WGR, Byte                        0.57  ==
  10%, WGR, Float64                     5.22  ===================
  
  15%, WGR, Byte                        0.89  ===
  15%, WGR, Float64                     7.56  ============================
  
Running benchmarks for Reading in large Extents of large files...

  Name                                     s  linear runtime
  55%, WGR, Byte                        0.29  =========
  
  60%, WGR, Byte                        0.57  ===================
  
  65%, WGR, Byte                        0.82  ============================
```

#### Understanding the Output: Large Files

Th first section also shows the results of reading in small extents on both the Byte and Float64 bandtypes. Notice though, that only the WindowedGeoTiffReader, "WGR", is used. This is because it is impossible to read in the whole file when they are this size. Thus, only the speed at which the two bandtypes are read is are comparred.

```console
Running benchmarks for Reading in small Extents of large files...

  Name                                     s  linear runtime
  5%, WGR, Byte                         0.29  =
  5%, WGR, Float64                      2.50  =========
  
  10%, WGR, Byte                        0.57  ==
  10%, WGR, Float64                     5.22  ===================
  
  15%, WGR, Byte                        0.89  ===
  15%, WGR, Float64                     7.56  ============================
```

While the second section is again reading in larger extents of the files, there is now even less being compared. Byte is the only bandtype that can be read in at these extents, and even still their max area that can be read is not the entire file. Rather, it is capped at around 65%.

```console
Running benchmarks for Reading in large Extents of large files...

  Name                                     s  linear runtime
  55%, WGR, Byte                        3.28  ========================
  60%, WGR, Byte                        3.57  ==========================
  65%, WGR, Byte                        3.91  ============================
```

There exists one other difference between the large and small files benchmark. It takes longer to run the tests using the larger files, as can be seen from the time. Which is now measured in seconds "s" and not miliseconds "ms".

#### Interpreting the Results: Large Files
One of the biggest things to take away from the large files benchmark is that the WindowedGeoTiffReader is capable of reading in files that would crash the traditional reader. This is important as it allows for easier reads of files without having to take additional steps to have them loaded.

Unlike with small files, the speed at which a bandtype is read in increases as the bandtype gets smaller. Bytes can be read in at almost 9x the speed as a Float64 at the smallest extent. Even as the size of the extent grows, the Byte bandtype can still read at much faster rates than Float64.

Not only do speeds increase at smaller bandtypes, but the area which could be read in grows as well. Float64s cannot be used to read in such large extents as what was shown previously. In fact, for this particular test file, the cap at which it can read is about 15% of the file. The exact cutoff for an extent before it causes an error is not known, however, estimated ranges for each have been produced have been created and can be found here [link].
