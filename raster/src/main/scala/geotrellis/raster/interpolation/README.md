# [Kriging Interpolation](https://en.wikipedia.org/wiki/Kriging)
This package is to be used for raster Kriging interpolation.

The process of Kriging interpolation is explained in the `geotrellis.vector.interpolation` package in detail in its `README.md`.

## Kriging Methods
The Kriging methods are largely classified into different types in the way the mean(mu) and the covariance values of the object are dealt with.

    //Array of sample points with given data
    val points: Array[PointFeature[Double]] = ...
    /** Supported is also present for
      * val points: Traversable[PointFeature[D]] = ... //where D <% Double
      */

    // The raster extent to be kriged
    val extent = Extent(xMin, yMin, xMax, yMax)
    int cols: Int = ...
    int rows: Int = ...
    val rasterExtent = RasterExtent(extent, cols, rows)

There exist four major kinds of Kriging interpolation techniques, namely :

#### Simple Kriging

    //Simple kriging, a tile  set with the Kriging prediction per cell is returned
    val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
    
    val krigingVal: Tile =
        points.simpleKriging(rasterExtent, 5000, sv)
        
    /**
      * The user can also do Simple Kriging using :
      * points.simpleKriging(rasterExtent)
      * points.simpleKriging(rasterExtent, bandwidth)
      * points.simpleKriging(rasterExtent, Semivariogram)
      * points.simpleKriging(rasterExtent, bandwidth, Semivariogram)
      */

It is belongs to the class of Simple Spatial Prediction Models.

The simple kriging is based on the assumption that the underlying stochastic process is entirely _known_ and the spatial trend is constant, viz. the mean and covariance values of the entire interpolation set is constant (using solely the sample points)

    mu(s) = mu              known; s belongs to R
    cov[eps(s), eps(s')]    known; s, s' belongs to R

#### Ordinary Kriging
    //Ordinary kriging, a tile  set with the Kriging prediction per cell is returned
    val sv: Semivariogram = NonLinearSemivariogram(points, 30000, 0, Spherical)
    
    val krigingVal: Tile =
        points.ordinaryKriging(rasterExtent, 5000, sv)
        
    /**
      * The user can also do Ordinary Kriging using :
      * points.ordinaryKriging(rasterExtent)
      * points.ordinaryKriging(rasterExtent, bandwidth)
      * points.ordinaryKriging(rasterExtent, Semivariogram)
      * points.ordinaryKriging(rasterExtent, bandwidth, Semivariogram)
      */

It is belongs to the class of Simple Spatial Prediction Models.

This method differs from the Simple Kriging appraoch in that, the constant mean is assumed to be unknown and is estimated within the model.

    mu(s) = mu              unknown; s belongs to R
    cov[eps(s), eps(s')]    known; s, s' belongs to R

#### Universal Kriging
    //Universal kriging, a tile  set with the Kriging prediction per cell is returned
    
    val attrFunc: (Double, Double) => Array[Double] = {
      (x, y) => Array(x, y, x * x, x * y, y * y)
    }
      
    val krigingVal: Tile = 
        points.universalKriging(rasterExtent, attrFunc, 50, Spherical)
      
    /**
      * The user can also do Universal Kriging using :
      * points.universalKriging(rasterExtent)
      * points.universalKriging(rasterExtent, bandwidth)
      * points.universalKriging(rasterExtent, model)
      * points.universalKriging(rasterExtent, bandwidth, model)
      * points.universalKriging(rasterExtent, attrFunc)
      * points.universalKriging(rasterExtent, attrFunc, bandwidth)
      * points.universalKriging(rasterExtent, attrFunc, model)
      * points.universalKriging(rasterExtent, attrFunc, bandwidth, model)
      */

It is belongs to the class of General Spatial Prediction Models.

This model allows for explicit variation in the trend function (mean function) constructed as a linear function of spatial attributes; with the covariance values assumed to be known. This model computes the prediction using

For example if :

    x(s) = [1, s1, s2, s1 * s1, s2 * s2, s1 * s2]'
    mu(s) = beta0 + beta1*s1 + beta2*s2 + beta3*s1*s1 + beta4*s2*s2 + beta5*s1*s2
Here, the "linear" refers to the linearity in parameters (beta).

    mu(s) = x(s)' * beta,   beta unknown; s belongs to R
    cov[eps(s), eps(s')]    known; s, s' belongs to R

#### Geostatistical Kriging
    //Geostatistical kriging, a tile  set with the Kriging prediction per cell is returned
    val attrFunc: (Double, Double) => Array[Double] = {
      (x, y) => Array(x, y, x * x, x * y, y * y)
    }
    
    val krigingVal: Tile = 
        points.geoKriging(rasterExtent, attrFunc, 50, Spherical)
      
    /**
      * The user can also do Universal Kriging using :
      * points.geoKriging(rasterExtent)
      * points.geoKriging(rasterExtent, bandwidth)
      * points.geoKriging(rasterExtent, model)
      * points.geoKriging(rasterExtent, bandwidth, model)
      * points.geoKriging(rasterExtent, attrFunc)
      * points.geoKriging(rasterExtent, attrFunc, bandwidth)
      * points.geoKriging(rasterExtent, attrFunc, model)
      * points.geoKriging(rasterExtent, attrFunc, bandwidth, model)
      */

It is belongs to the class of General Spatial Prediction Models.

This model relaxes the assumption that the covariance is known.
Thus, the beta values and covariances are simultaneously evaluated and is computationally more intensive.

    mu(s) = x(s)' * beta,   beta unknown; s belongs to R
    cov[eps(s), eps(s')]    unknown; s, s' belongs to R


### attribute Functions (Universal, Geostatistical Kriging): 

The `attrFunc` function is the attribute function, which is used for evaluating non-constant spatial trend structures. Unlike the Simple and Ordinary Kriging models which rely only on the residual values for evaluating the spatial structures, the General Spatial Models may be modelled by the user based on the data (viz. evaluating the beta variable to be used for interpolation).

In case the user does not specify an attribute function, by default the function used is a quadratic trend function for Point(s1, s2) :

```mu(s) = beta0 + beta1*s1 + beta2*s2 + beta3*s1*s1 + beta4*s2*s2 + beta5*s1*s2```

General example of a trend function is : 

```mu(s) = beta0 + Sigma[ beta_j * (s1^n_j) * (s2^m_j) ]```

### Example to understand the attribute Functions

Consider a problem statement of interpolating the ground water levels of Venice. It is easy to arrive at the conclusion that it depends on three major factors; namely, the elevation from the ground, the industries' water intake, the island's water consumption.
First of all, we would like to map the coordinate system into another coordinate system such that generation of the relevant attributes becomes easier (please note that the user may use any method for generating the set of attribute functions; in this case we have used coordinate transformation before the actual calculation).

    val c1: Double = 0.01 * (0.873 * (x - 418) - 0.488 * (y - 458))
    val c2: Double = 0.01 * (0.488 * (x - 418) + 0.873 * (y - 458))

![Coordinate Mapping](illustration/coordinateMapping.png)  


#### Elevation



    /** Estimate of the elevation's contribution to groundwater level 
      * [10 * exp(-c1)]
      */
    val elevation: Double = math.exp(-1 * c1)
    

#### Industry draw down (water usage of industry)

![Industry Draw Down](illustration/industryDrawDown.png)  



    /** Estimate of the industries' contribution to groundwater level 
      * exp{ -1.0 * [(1.5)*c1^2 - c2^2]}
      */
    val industryDrawDown: Double = math.exp(-1.5 * c1 * c1 - c2 * c2)

#### Island draw down (water usage of Venice)

![Venice Draw Down](illustration/veniceDrawDown.png)  

    /** Estimate of the island's contribution to groundwater level 
      * //exp{-1.0 * (sqrt((s1-560)^2 + (s2-390)^2) / 35)^8 }
      */
    val islandDrawDown: Double = 
        math.exp(-1 * math.pow(math.sqrt(math.pow(x - 560, 2) + math.pow(y - 390, 2)) / 35, 8))

#### The final attribute Function

Thus for a Point(s1, s2) : 

`Array(elevation, industryDrawDown, islandDrawDown)` is the set of attributes.

In case the intuition for a relevant `attrFunc` is not clear; the user need not supply an `attrFunc`, by default the following attribute Function is used :

    //For a Point(x, y), the set of default attributes is :
    Array(x, y, x * x, x * y, y * y)

The default function would use the data values of the given sample points and construct a spatial structure trying to mirror the actual attribute characteristics.
