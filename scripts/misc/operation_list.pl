use strict;

my $fh;
open(my $fh, "<", "target/scala-2.9.1/api/trellis/operation/package.html");

my ($name, $op, $output, $description) = '';

my $simple_name = '';
print <<EOT;
    <table class="bordered-table zebra-striped">
      <thead>
          <tr>
            <th>Operation</th>
            <th>Output</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
EOT

while (<$fh>) {
  my $line = $_;

  if ($_ =~ /class="name">([^<]*)</) {
    my $class_name = $1;
    $simple_name = $class_name;
    $name = "<a href=\"http://azavea.github.com/trellis/latest/api/index.html#trellis.operation.$class_name\">$class_name</a>";
    $output = '';
    $description = '';
#    print ("name: " . $name . "\n");
  }

  if ($_ =~ /Op.*\[([^\]]*)\]/) {
    my $types = $1;
    $output = $types;
    if ($types =~ /, ([^,]*)$/) {
      $output = $1;
      $output =~ s/IntRaster/Raster/g;
    }
   #print ("output: " . $output . "\n");
  }

  if ($_ =~ /cmt">([^<]*)<\/p/) {
    $description = $1;
    #print ("description: " . $description . "\n");
  } else {
    if ($_ =~ /cmt">(.*)/) {
      $description = $1 . " ...";
      #print ("multi line description: " . $description . "\n");
    }
  } 

  if ($name ne '' and $output ne '' and $description ne '') {
    print("<tr>");
    print ("<td><code>$name</code></td>");
    print ("<td>$output</td>");
    print ("<td>$description</td>");
    print ("</tr>\n");
    $name = '';
    $output = '';
    $description = '';
  }
}


print <<EOT;
        </tbody>
      </table>
EOT
