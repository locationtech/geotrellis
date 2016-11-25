/*
 * Copyright 2016 Martin Davis, Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgeo.proj4j.datum;

import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.util.IntPolarCoordinate;
import org.osgeo.proj4j.util.FloatPolarCoordinate;
import org.osgeo.proj4j.util.PolarCoordinate;
import org.osgeo.proj4j.util.ProjectionMath;

/**
 * A Grid represents a geodetic datum defining some mapping between a
 * coordinate system referenced to the surface of the earth and spherical
 * coordinates.  Generally Grids are loaded from definition files in the proj4
 * resource directory.
 */
// Grid corresponds to the PJ_GRIDINFO struct in proj.4
public final class Grid implements Serializable {
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

    /**
     * Merge (append) a named grid into the given gridlist.
     */
    // This method corresponds to the pj_gridlist_merge_gridfile function in proj.4
    public static void mergeGridFile(
        String name,
        List<Grid> gridList)
    throws IOException
    {
        gridList.add(gridinfoInit(name));
        // TODO: Maintain cache of loaded grids so we never need more than one
        // copy of the same grid file loaded in memory
    }

    /**
     * Convert between this grid and WGS84, or vice versa if the <code>inverse</code> flag is set.
     */
    // This method corresponds to the pj_apply_gridshift function from proj.4
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

        if (! Double.isNaN(output.lam)) {
            in.x = output.lam;
            in.y = output.phi;
        } else {
            // Proj.4 guards this with #ifdef ERR_GRID_AREA_TRANSIENT_SEVERE
            // in.x = in.y = Double.NaN;
        }
    }

    // This class corresponds to the CTABLE struct from proj.4
    public static final class ConversionTable implements Serializable {
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

        @Override
        public int hashCode() {
            int idHash = id == null ? 0 : id.hashCode();
            int delHash = del == null ? 0 : del.hashCode();
            int llHash = ll == null ? 0 : ll.hashCode();
            int cvsHash = Arrays.hashCode(cvs);
            return idHash | (11 * delHash) | (23 * llHash) | (37 * cvsHash);
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof ConversionTable) {
                ConversionTable ct = (ConversionTable) that;
                if (id == null && ct.id != null) return false;
                if (! id.equals(ct.id)) return false;
                if (del == null && ct.del != null) return false;
                if (! del.equals(ct.del)) return false;
                if (ll == null && ct.ll != null) return false;
                if (! ll.equals(ct.ll)) return false;
                if (! Arrays.equals(cvs, ct.cvs)) return false;
                return true;
            } else { 
                return false;
            }
        }
    }

    // This method corresponds to the nad_cvt function in proj.4
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

    // This method corresponds to the nad_intr method in proj.4
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


    // This method corresponds to the pj_gridlist_from_nadgrids function in proj.4
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

    // This method corresponds to the pj_gridinfo_init function in proj.4
    private static Grid gridinfoInit(String gridName) throws IOException {
        Grid grid = new Grid();
        grid.gridName = gridName;
        grid.format = "missing";
        grid.gridOffset = 0;
        DataInputStream gridDefinition = resolveGridDefinition(gridName);
        if (gridDefinition == null) {
            throw new IOException("Unknown grid: " + gridName);
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
        if (NTV1.testHeader(header)) {
            grid.format = "ntv1";
            gridDefinition.mark(1024);
            grid.table = NTV1.init(gridDefinition);
            gridDefinition.reset();
            NTV1.load(gridDefinition, grid);
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

    @Override
    public int hashCode() {
        int nameHash = gridName == null ? 0 : gridName.hashCode();
        int fileHash = fileName == null ? 0 : fileName.hashCode();
        int formatHash = format == null ? 0 : format.hashCode();
        int tableHash = table == null ? 0 : table.hashCode();
        int nextHash = next == null ? 0 : next.hashCode();
        int childHash = next == null ? 0 : next.hashCode();
        return nameHash | (7 * fileHash) | (11 * formatHash) | (17 * tableHash) | (23 * nextHash) | (31 * childHash);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof Grid) {
            Grid g = (Grid) that;
            if (gridName == null && g.gridName != null) return false;
            if (!gridName.equals(g.gridName)) return false;
            if (fileName == null && g.fileName != null) return false;
            if (!fileName.equals(g.fileName)) return false;
            if (format == null && g.format != null) return false;
            if (!format.equals(g.format)) return false;
            if (table == null && g.table != null) return false;
            if (!table.equals(g.table)) return false;
            if (next == null && g.next != null) return false;
            if (!next.equals(g.next)) return false;
            if (child == null && g.child != null) return false;
            if (!child.equals(g.child)) return false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Grid[" + gridName + "; " + format + "]";
    }
}
