package org.osgeo.proj4j;

import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgeo.proj4j.util.ProjectionMath;

public final class Grid {
    /**
     * Identifying name for this Grid. eg "conus" or ntv2_0.gsb
     */
    private String gridName;

    /**
     * URI for accessing the grid definition file
     */
    private String fileName;

    /**
     * File format of the grid definition file, ie "ctable", "ntv1", "ntv2" or "missing"
     */
    private String format;

    private int gridOffset; // Offset in file of the grid definition, for delayed loading

    final static int MAX_TRY = 9; // maximum number of iterations for nad conversion algorithm
    final static double TOL = 1e-12; // tolerance for nad conversion algorithm

    ConversionTable table;

    private Grid next;
    private Grid child;

    public static void mergeGridFile(
        String name,
        List<Grid> gridList)
    throws IOException
    {
        boolean gotMatch = false;
        Grid tail = null, thisGrid = null;
        if (gridList.size() > 0) {
            for (thisGrid = gridList.get(0); thisGrid != null; thisGrid = thisGrid.next) {
                if (thisGrid.gridName.equals(name)) {
                    gotMatch = true;
                    if (thisGrid.table == null) return;
                    gridList.add(thisGrid);
                }

                tail = thisGrid;
            }
        }

        if (gotMatch) return;
        thisGrid = gridinfoInit(name);

        if (thisGrid == null) {
            throw null;
        }

        if (tail != null)
            tail.next = thisGrid;
        else
            gridList.add(thisGrid);

        mergeGridFile(name, gridList);
    }

    public static void shift(List<Grid> grids, boolean inverse, ProjCoordinate in) {
        PolarCoordinate input = new PolarCoordinate(in.x, in.y),
                        output = new PolarCoordinate(Double.NaN, Double.NaN);

        for (Grid grid : grids) {
            ConversionTable table = grid.table;
            double epsilon = (Math.abs(table.del.phi) + Math.abs(table.del.lam)) / 10000d;
            // Skip tables that don't match our point at all
            if (table.ll.phi - epsilon > input.phi
                    || table.ll.lam - epsilon > input.lam
                    || (table.ll.phi + (table.lim.phi - 1) * table.del.phi + epsilon < input.phi)
                    || (table.ll.lam + (table.lim.lam - 1) * table.del.lam + epsilon < input.lam))
            continue;

            // If we have child nodes, check to see if any of them apply
            while (grid.child != null)  {
                Grid child;
                for (child = grid.child; child != null; child = child.next) {
                    ConversionTable t = child.table;
                    double epsilon0 = (Math.abs(t.del.phi) + Math.abs(t.del.lam)) / 10000d;

                    if (t.ll.phi - epsilon0 > input.phi
                            || t.ll.lam - epsilon0 > input.lam
                            || (t.ll.phi + (t.lim.phi - 1) * t.del.phi + epsilon0 < input.phi)
                            || (t.ll.lam + (t.lim.lam - 1) * t.del.lam + epsilon0 < input.lam))
                    continue;

                    break;
                }

                if (child == null) break;

                grid = child;
            }

            if (grid.table.cvs == null) {
                // proj.4 only reads headers when 'initializing' a grid and
                // loads the grid itself here if needed. for now we're just
                // loading the whole grid at once so this is a no-op
                // loadConversionTable(table);
            }

            output = nad_cvt(input, inverse, table);
        }

        if (Double.isNaN(output.lam)) {
            in.x = in.y = Double.NaN;
        } else {
            in.x = output.lam;
            in.y = output.phi;
        }
    }

    public static final class ConversionTable {
        /**
         * ASCII info
         */
        public String id;
        /**
         * Lower left corner coordinates
         */
        public PolarCoordinate del;
        /**
         * Cell size
         */
        public PolarCoordinate ll;
        /**
         * Size of conversion matrix (number of rows/columns)
         */
        public IntPolarCoordinate lim;
        /**
         * Conversion matrix
         */
        public FloatPolarCoordinate[] cvs;

        @Override
        public String toString() {
            return String.format("Grid: %s", id);
        }
    }

    private static PolarCoordinate nad_cvt(PolarCoordinate in, boolean inverse, ConversionTable table) {
        PolarCoordinate t, tb;
        if (Double.isNaN(in.lam))
            return in;

        tb = new PolarCoordinate(in);
        tb.lam -= table.ll.lam;
        tb.phi -= table.ll.phi;
        tb.lam = ProjectionMath.normalizeLongitude(tb.lam - Math.PI) + Math.PI;
        t = nad_intr(tb, table);

        if (inverse) {
            PolarCoordinate del = new PolarCoordinate(Double.NaN, Double.NaN),
                            dif = new PolarCoordinate(Double.NaN, Double.NaN);
            int i = MAX_TRY;

            if (Double.isNaN(t.lam)) return t;
            t.lam = tb.lam + t.lam;
            t.phi = tb.phi - t.phi;

            do {
                del = nad_intr(t, table);
                if (Double.isNaN(del.lam)) {
                    // TODO: LOG
                    // fprintf( stderr, 
                    //          "Inverse grid shift iteration failed, presumably at grid edge.\n"
                    //          "Using first approximation.\n" );
                    break;
                }
                t.lam -= dif.lam = t.lam - del.lam - tb.lam;
                t.phi -= dif.phi = t.phi + del.phi - tb.phi;
            } while (i-- > 0 && Math.abs(dif.lam) > TOL && Math.abs(dif.phi) > TOL);

            if (i < 0) {
                // TODO: Log
                // fprintf( stderr, 
                //          "Inverse grid shift iterator failed to converge.\n" );
                t.lam = t.phi = Double.NaN;
                return t;
            }
            in.lam = ProjectionMath.normalizeLongitude(t.lam + table.ll.lam);
            in.phi = t.phi + table.ll.phi;
        } else {
            if (Double.isNaN(t.lam)) {
                in = t;
            } else {
                in.lam -= t.lam;
                in.phi += t.phi;
            }
        }
        return in;
    }

    private static PolarCoordinate nad_intr(PolarCoordinate t, ConversionTable table) {
        t = new PolarCoordinate(t);
        PolarCoordinate val = new PolarCoordinate(Double.NaN, Double.NaN);
        IntPolarCoordinate indx = new IntPolarCoordinate(
                (int) Math.floor(t.lam /= table.del.lam), 
                (int) Math.floor(t.phi /= table.del.phi));
        PolarCoordinate frct = new PolarCoordinate(t.lam - indx.lam, t.phi - indx.phi);
        double m00, m10, m01, m11;
        FloatPolarCoordinate f00, f10, f01, f11;
        int index;
        int in;

        if (indx.lam < 0) {
            if (indx.lam == -1 && frct.lam > 0.99999999999) {
                ++indx.lam;
                frct.lam = 0d;
            } else {
                return val;
            }
        } else if ((in = indx.lam + 1) >= table.lim.lam) {
            if (in == table.lim.lam && frct.lam < 1e-11) {
                --indx.lam;
                frct.lam = 1d;
            } else {
                return val;
            }
        }
        if (indx.phi < 0) {
            if (indx.phi == -1 && frct.phi > 0.99999999999) {
                ++indx.phi;
                frct.phi = 0d;
            } else {
                return val;
            }
        } else if ((in = indx.phi + 1) >= table.lim.phi) {
            if (in == table.lim.phi && frct.phi < 1e-11) {
                --indx.phi;
                frct.phi = 1d;
            } else {
                return val;
            }
        }
        index = indx.phi * ((int)table.lim.lam) + indx.lam;
        f00 = table.cvs[index++];
        f10 = table.cvs[index];
        index += table.lim.lam;
        f11 = table.cvs[index--];
        f01 = table.cvs[index];
        m11 = m10 = frct.lam;
        m00 = m01 = 1d - frct.lam;
        m11 *= frct.phi;
        m01 *= frct.phi;
        val.lam = m00 * f00.lam + m10 * f10.lam + m01 * f01.lam + m11 * f11.lam;
        val.phi = m00 * f00.phi + m10 * f10.phi + m01 * f01.phi + m11 * f11.phi;
        return val;
    }

    public static List<Grid> fromNadGrids(String grids) throws IOException {
        List<Grid> gridlist = new ArrayList<Grid>();
        synchronized(Grid.class) {
            for (String gridName : grids.split(",")) {
                boolean optional = gridName.startsWith("@");
                if (optional) gridName = gridName.substring(1);
                try {
                    mergeGridFile(gridName, gridlist);
                } catch (IOException e) {
                    if (!optional) throw e;
                }
            }
        }
        return gridlist;
    }

    private static Grid gridinfoInit(String gridName) throws IOException {
        Grid grid = new Grid();
        grid.gridName = gridName;
        grid.format = "missing";
        grid.gridOffset = 0;
        DataInputStream gridDefinition = resolveGridDefinition(gridName);
        if (gridDefinition == null) {
            throw new Error("Unknown grid: " + gridName);
        }
        byte[] header = new byte[160];
        gridDefinition.mark(header.length);
        gridDefinition.readFully(header);
        gridDefinition.reset();
        if (CTABLEV2.testHeader(header)) {
            grid.format = "ctable2";
            gridDefinition.mark(1024);
            grid.table = CTABLEV2.init(gridDefinition);
            gridDefinition.reset();
            CTABLEV2.load(gridDefinition, grid);
        }
        return grid;
    }

    private static DataInputStream resolveGridDefinition(String gridName) throws IOException {
        // proj.4 also has a couple of environment variables that influence the
        // search path for grid definition files, but for now we only check the
        // working directory and the classpath (in that order.)
        File file = new File(gridName);
        if (file.exists()) return new DataInputStream(new FileInputStream(file));
        InputStream resource = Grid.class.getResourceAsStream("/geotrellis/proj4/nad/" + gridName);
        if (resource != null) return new DataInputStream(resource);

        return null;
    }
}
