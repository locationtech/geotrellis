GeoTrellis supports two well-known distributed vector-feature stores:
GeoWave and GeoMesa. A question that often arises in the vector processing
world is: "Which should I use?" At first glance, it can be hard to tell the
difference, apart from "one is Java and the other is Scala". The real answer
is, of course, "it depends".

In the fall of 2016, our team was tasked with an official comparison of the
two. It was our goal to increase awareness of their respective strengths and
weaknesses, so that both teams can focus on their strengths during
development, and the public can make an easier choice. We analysed a
number of angles, including:

- Feature set
- Performance
- Ease of use
- Project maturity

[Click here for the full report.](#)

Keep in mind that as of 2016 October 25, both of these GeoTrellis modules
are still experimental.

GeoMesa
-------

**Choose GeoMesa if:** you have a smaller dataset?

[GeoMesa](http://www.geomesa.org/)

GeoWave
-------

**Choose GeoWave if:** you plan to perform many concurrent queries in
production.

[GeoWave](https://github.com/ngageoint/geowave)
