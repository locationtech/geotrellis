// file boilerplate/main.js

define([
    'base/js/namespace'
], function(
    Jupyter
) {
    function load_ipython_extension() {
        var handler = function () {
            var spark_cell = Jupyter.notebook.insert_cell_above('code', 0);
            spark_cell.set_text('import $exclude.`org.slf4j:slf4j-log4j12`, $ivy.`org.slf4j:slf4j-nop:1.7.21`  // Quiets the logger\n\
import $profile.`hadoop-2.6`\n\
import $ivy.`org.apache.spark::spark-sql:2.1.0`\n\
import $ivy.`org.apache.hadoop:hadoop-aws:2.6.4`\n\
import $ivy.`org.jupyter-scala::spark:0.4.2`\n\
\n\
import org.apache.spark._\n\
import org.apache.spark.rdd.RDD\n\
import jupyter.spark.session._\n\
\n\
val sparkSession =\n\
  JupyterSparkSession\n\
    .builder()\n\
    .jupyter() // Must be called immediately after builder()\n\
    .master("local[*]")\n\
    .appName("testing")\n\
.getOrCreate()\n\
\n\
implicit val sc = sparkSession.sparkContext');
            var lt_gt_cell = Jupyter.notebook.insert_cell_below('code', 0);
            lt_gt_cell.set_text('// Execute to load GeoTrellis from LocationTech repo\n\
\n\
import ammonite._, Resolvers._\n\
\n\
val locationtech = Resolver.Http(\n\
    "locationtech-releases",\n\
    "https://repo.locationtech.org/content/groups/releases",\n\
    MavenPattern,\n\
    true  // Declares whether the organization is dot- (false) or slash- (true) delimited\n\
)\n\
\n\
interp.resolvers() = interp.resolvers() :+ locationtech\n\
\n\
import $ivy.`org.locationtech.geotrellis::geotrellis-spark-etl:1.2.0`');
            var local_gt_cell = Jupyter.notebook.insert_cell_below('code', 1);
            local_gt_cell.set_text('// Execute to load GeoTrellis from a fat jar uploaded to the local file system\n\
\n\
import ammonite._\n\
interp.load.cp(ops.Path("/tmp/geotrellis-spark-etl-assembly-1.2.0-SNAPSHOT.jar"))');
       };

        var action = {
            icon       : 'fa-star-o', // a font-awesome class used on buttons, etc
            help       : 'Insert standard boilerplate',
            help_index : 'zz',
            handler    : handler
        };
        var prefix = 'boilerplate';
        var action_name = 'insert_standard_code';

        var full_action_name = Jupyter.actions.register(action, action_name, prefix);
        Jupyter.toolbar.add_buttons_group([full_action_name]);
    }

    return {
        load_ipython_extension: load_ipython_extension
    };
});
