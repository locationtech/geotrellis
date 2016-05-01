/* THIS WILL NOT RUN
 * Below, there are a few examples of how the test files were generated using
 * java-vector-tile.
 * https://github.com/ElectronicChartCentre/java-vector-tile
 */

public class GenTestSets {

    private GeometryFactory gf = new GeometryFactory();

    public void SingleLayer() {
        VectorTileEncoder vtm = new VectorTileEncoder(256);

        List<Coordinate> cs = new ArrayList<Coordinate>();
        cs.add(new Coordinate(3, 6));
        cs.add(new Coordinate(8, 12));
        cs.add(new Coordinate(20, 34));
        Geometry geometry = gf.createLineString(cs.toArray(new Coordinate[cs.size()]));

        Map<String, Object> attributes = new HashMap<String, Object>();

        vtm.addFeature("DEPCNT", attributes, geometry);

        byte[] encoded = vtm.encode();
        try {
            File file = new File("SingleLayer.mvt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(encoded);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void MultiLayer() {
        VectorTileEncoder vtm = new VectorTileEncoder(256);

        List<Coordinate> cs = new ArrayList<Coordinate>();
        cs.add(new Coordinate(3, 6));
        cs.add(new Coordinate(8, 12));
        cs.add(new Coordinate(20, 34));
        Geometry geometry = gf.createLineString(cs.toArray(new Coordinate[cs.size()]));

        cs.add(new Coordinate(33, 72));
        Geometry geometry2 = gf.createLineString(cs.toArray(new Coordinate[cs.size()]));

        Map<String, Object> attributes = new HashMap<String, Object>();

        vtm.addFeature("DEPCNT", attributes, geometry);
        vtm.addFeature("TNCPED", attributes, geometry2);

        byte[] encoded = vtm.encode();
        try {
            File file = new File("MultiLayer.mvt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(encoded);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void GrabBag() {
        VectorTileEncoder vtm = new VectorTileEncoder(256);

        List<Coordinate> cs = new ArrayList<Coordinate>();
        List<LineString> ls = new ArrayList<LineString>();
        List<Polygon> ps = new ArrayList<Polygon>();

        // point
        Geometry point = gf.createPoint(new Coordinate(200, 200));

        // multipoint
        cs.add(new Coordinate(50, 50));
        cs.add(new Coordinate(60, 60));
        cs.add(new Coordinate(70, 70));
        Geometry points = gf.createMultiPoint(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();

        // line
        cs.add(new Coordinate(3, 6));
        cs.add(new Coordinate(8, 12));
        cs.add(new Coordinate(20, 34));
        Geometry line = gf.createLineString(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();

        // multiline
        cs.add(new Coordinate(2, 4));
        cs.add(new Coordinate(6, 3));
        cs.add(new Coordinate(1, 17));
        LineString l1 = gf.createLineString(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();
        cs.add(new Coordinate(5, 3));
        cs.add(new Coordinate(7, 2));
        cs.add(new Coordinate(17, 1));
        LineString l2 = gf.createLineString(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();
        ls.add(l1);
        ls.add(l2);
        Geometry lines = gf.createMultiLineString(ls.toArray(new LineString[ls.size()]));
        ls.clear();

        // polygon
        cs.add(new Coordinate(5, 10));
        cs.add(new Coordinate(10, 20));
        cs.add(new Coordinate(5, 30));
        cs.add(new Coordinate(5, 10));
        Geometry polygon = gf.createPolygon(
                gf.createLinearRing(cs.toArray(new Coordinate[cs.size()])),
                null
        );
        cs.clear();

        // multipolygon
        cs.add(new Coordinate(5, 10));
        cs.add(new Coordinate(10, 20));
        cs.add(new Coordinate(5, 30));
        cs.add(new Coordinate(5, 10));
        Polygon p1 = gf.createPolygon(
                gf.createLinearRing(cs.toArray(new Coordinate[cs.size()])),
                null
        );
        cs.clear();
        cs.add(new Coordinate(30, 5));
        cs.add(new Coordinate(20, 75));
        cs.add(new Coordinate(70, 30));
        cs.add(new Coordinate(30, 5));
        Polygon p2 = gf.createPolygon(
                gf.createLinearRing(cs.toArray(new Coordinate[cs.size()])),
                null
        );
        cs.clear();
        ps.add(p1);
        ps.add(p2);
        Geometry polygons = gf.createMultiPolygon(ps.toArray(new Polygon[ps.size()]));
        ps.clear();

        Map<String, Object> attributes = new HashMap<String, Object>();
        vtm.addFeature("DEPCNT", attributes, point);
        vtm.addFeature("DEPCNT", attributes, points);
        vtm.addFeature("DEPCNT", attributes, line);
        vtm.addFeature("DEPCNT", attributes, lines);
        vtm.addFeature("DEPCNT", attributes, polygon);
        vtm.addFeature("DEPCNT", attributes, polygons);

        byte[] encoded = vtm.encode();
        try {
            File file = new File("GrabBag.mvt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(encoded);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void PolygonWithHole() {
        VectorTileEncoder vtm = new VectorTileEncoder(256);

        List<LinearRing> holes = new ArrayList<LinearRing>();

        List<Coordinate> cs = new ArrayList<Coordinate>();
        cs.add(new Coordinate(0, 0));
        cs.add(new Coordinate(500, 0));
        cs.add(new Coordinate(500, 500));
        cs.add(new Coordinate(0, 500));
        cs.add(new Coordinate(0, 0));
        LinearRing lr = gf.createLinearRing(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();
        cs.add(new Coordinate(5, 5));
        cs.add(new Coordinate(10, 5));
        cs.add(new Coordinate(10, 10));
        cs.add(new Coordinate(5, 5));
        LinearRing h1 = gf.createLinearRing(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();
        cs.add(new Coordinate(70, 70));
        cs.add(new Coordinate(140, 30));
        cs.add(new Coordinate(170, 50));
        cs.add(new Coordinate(70, 70));
        LinearRing h2 = gf.createLinearRing(cs.toArray(new Coordinate[cs.size()]));
        cs.clear();
        holes.add(h1);
        holes.add(h2);

        Geometry geometry = gf.createPolygon(
                lr,
                holes.toArray(new LinearRing[holes.size()])
        );

        Map<String, Object> attributes = new HashMap<String, Object>();

        vtm.addFeature("DEPCNT", attributes, geometry);

        byte[] encoded = vtm.encode();
        try {
            File file = new File("PolygonWithHole.mvt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(encoded);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Tagged() throws IOException {
        VectorTileEncoder vtm = new VectorTileEncoder(256);
        Geometry geometry = gf.createPoint(new Coordinate(3, 6));

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("key1", "value1");
        attributes.put("key2", Integer.valueOf(123));
        attributes.put("key3", Float.valueOf(234.1f));
        attributes.put("key4", Double.valueOf(567.123d));
        attributes.put("key5", Long.valueOf(-123));
        attributes.put("key6", "value6");

        vtm.addFeature("DEPCNT", attributes, geometry);

        byte[] encoded = vtm.encode();
        try {
            File file = new File("Tagged.mvt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(encoded);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
   }

}
