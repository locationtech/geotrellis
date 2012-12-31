simpleBarGraphTiltedLabels("../resources/dat/degrees.csv","#degrees","Percentage",450,240,undefined,"#CF5300",{top: 20, right: 20, bottom: 60, left: 40})
simpleBarGraphTiltedLabels("../resources/dat/worth-it.csv","#worthit","Percentage",250,250,70)
simpleBarGraphTiltedLabels("../resources/dat/followup.csv","#followup","Percentage",250,250,70,"#5E4175")

groupedBarGraph("../resources/dat/languages-percentages.csv","#languages-percentages","Percentage")
groupedBarGraph("../resources/dat/difficulty-to-expertise.csv","#difficulty-to-expertise","Percentage",450,240)
groupedBarGraph("../resources/dat/difficulty-to-field.csv","#difficulty-to-field","Percentage",450,240)
groupedBarGraph("../resources/dat/editors.csv","#editors","Percentage",450,240)
groupedBarGraph("../resources/dat/difficulty-to-education.csv", '#difficulty-to-education', 'Percentage', '800', '320')

verticalBarGraph("../resources/dat/fields-of-study.csv","#fields-of-study")

function groupedBarGraph(pathToCsv,div,ylabel,w,h,colorArr,margin,yformat) {
  div = typeof div !== 'undefined' ? div : "body";
  ylabel = typeof ylabel !== 'undefined' ? ylabel : "";
  w = typeof w !== 'undefined' ? w : 960;
  h = typeof h !== 'undefined' ? h : 480;
  colorArr = typeof colorArr !== 'undefined' ? colorArr : ["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"];
  margin = typeof margin != 'undefined' ? margin : {top: 20, right: 20, bottom: 30, left: 40};
  yformat = typeof format !== 'undefined' ? format : d3.format(".2s"); // for example, for %ages d3.format(".0%");

  var width = w - margin.left - margin.right,
      height = h - margin.top - margin.bottom;

  var x0 = d3.scale.ordinal()
      .rangeRoundBands([0, width], .1);

  var x1 = d3.scale.ordinal();

  var y = d3.scale.linear()
      .range([height, 0]);

  var color = d3.scale.ordinal()
      .range(colorArr);

  var xAxis = d3.svg.axis()
      .scale(x0)
      .orient("bottom");

  var yAxis = d3.svg.axis()
      .scale(y)
      .orient("left")
      .tickFormat(yformat);

  var svg = d3.select(div).append("svg")
      .attr("width", width + margin.left + margin.right)
      .attr("height", height + margin.top + margin.bottom)
    .append("g")
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  d3.csv(pathToCsv, function(error, data) {
    var expLevels = d3.keys(data[0]).filter(function(key) { return key !== "Key"; });

    data.forEach(function(d) {
      d.exp = expLevels.map(function(name) { return {name: name, value: +d[name]}; });
    });

    x0.domain(data.map(function(d) { return d.Key; }));
    x1.domain(expLevels).rangeRoundBands([0, x0.rangeBand()]);
    y.domain([0, d3.max(data, function(d) { return d3.max(d.exp, function(d) { return d.value; }); })]);

    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    svg.append("g")
        .attr("class", "y axis")
        .call(yAxis)
      .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", 6)
        .attr("dy", ".71em")
        .style("text-anchor", "end")
        .text(ylabel)
        .style("font-weight", "bold");

    var language = svg.selectAll(".language")
        .data(data)
      .enter().append("g")
        .attr("class", "g")
        .attr("transform", function(d) { return "translate(" + x0(d.Key) + ",0)"; })
        .style("font-size", "10px");

    language.selectAll("rect")
        .data(function(d) { return d.exp; })
      .enter().append("rect")
        .attr("width", x1.rangeBand())
        .attr("x", function(d) { return x1(d.name); })
        .attr("y", function(d) { return y(d.value); })
        .attr("height", function(d) { return height - y(d.value); })
        .style("fill", function(d) { return color(d.name); })
        .on ("mouseover",mover)
        .on ("mouseout",mout);

    if (ylabel.toLowerCase().indexOf("percentage") != -1) { var suffix = "%"; }
    else { var suffix = ""; }

    function mover(d) {
      d3.select(this).transition().attr("height", function(d) { return height - y(d.value) + 5; })
        .attr("y", function(d) { return y(d.value) - 5;})
        .attr("width", x1.rangeBand() + 2)
        .attr("x", function(d) { return x1(d.name) - 1; })
        .style("fill-opacity", .9);
      $(this).attr("rel","twipsy")
        .attr("data-original-title",d.value + suffix)
        .twipsy('show');
    }

    function mout(d) {
      d3.select(this).transition().attr("height", function(d) { return height - y(d.value); })
        .attr("y", function(d) { return y(d.value); })
        .attr("width", x1.rangeBand())
        .attr("x", function(d) { return x1(d.name); })
        .style("fill-opacity", 1);
      $(this).twipsy('hide');
    }

    $("rect[rel=twipsy]").twipsy({ live: true, offset: 4 });

    var legend = svg.selectAll(".legend")
        .data(expLevels.slice())
      .enter().append("g")
        .attr("class", "legend")
        .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

    legend.append("rect")
        .attr("x", width - 18)
        .attr("width", 18)
        .attr("height", 18)
        .style("fill", color);

    legend.append("text")
        .attr("x", width - 24)
        .attr("y", 9)
        .attr("dy", ".35em")
        .style("text-anchor", "end")
        .text(function(d) { return d; });

  });
}

function simpleBarGraphTiltedLabels(pathToCsv,div,ylabel,w,h,maxy,color,margin,yformat) {
    return simpleBarGraph(pathToCsv,div,ylabel,w,h,maxy,color,margin,yformat,true);
}

function simpleBarGraph(pathToCsv,div,ylabel,w,h,maxy,color,margin,yformat,tilted) {
  div = typeof div !== 'undefined' ? div : "body";
  w = typeof w !== 'undefined' ? w : 960;
  h = typeof h !== 'undefined' ? w : 480;
  ylabel = typeof ylabel !== 'undefined' ? ylabel : "";
  //maxy's default is set below
  color = typeof color !== 'undefined' ? color : "steelblue";
  margin = typeof margin != 'undefined' ? margin : {top: 20, right: 20, bottom: 80, left: 40};
  yformat = typeof format !== 'undefined' ? format : d3.format(".2s"); // for example, for %ages d3.format(".0%");
  tilted = typeof tilted !== 'undefined' ? tilted : false;

  var width = w - margin.left - margin.right,
      height = h - margin.top - margin.bottom;

  var x = d3.scale.ordinal()
      .rangeRoundBands([0, width], .1);

  var y = d3.scale.linear()
      .range([height, 0]);

  var xAxis = d3.svg.axis()
      .scale(x)
      .orient("bottom");

  var yAxis = d3.svg.axis()
      .scale(y)
      .orient("left")
      .tickFormat(yformat);

  var svg = d3.select(div).append("svg")
      .attr("width", width + margin.left + margin.right)
      .attr("height", height + margin.top + margin.bottom)
    .append("g")
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  var bargraph = d3.csv(pathToCsv, function(error, data) {
    data.forEach(function(d) {
      d.el2 = +d.el2;
    });

    x.domain(data.map(function(d) { return d.el1; }));
    if (typeof maxy !== 'undefined') {
      y.domain([0, maxy]);
    } else {
      y.domain([0, d3.max(data, function(d) { return d.el2; })]);
    }

    var xax = svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    if(tilted) {
      xax.selectAll("text")
      .attr("text-anchor", function(d) { return "end" })
      .attr("transform", function(d) { return "translate(-20,20)rotate(-45)"; })
      .style("font-size","11px")
      .style("font-weight","bold");
    }

    svg.append("g")
        .attr("class", "y axis")
        .call(yAxis)
      .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", 6)
        .attr("dy", ".71em")
        .style("text-anchor", "end")
        .text("Percentage")
        .style("font-weight","bold");

    svg.selectAll(".bar")
        .data(data)
      .enter().append("rect")
        .attr("class", "bar")
        .attr("x", function(d) { return x(d.el1); })
        .attr("width", x.rangeBand())
        .attr("y", function(d) { return y(d.el2); })
        .attr("height", function(d) { return height - y(d.el2); })
        .style("fill", color)
        .on ("mouseover",mover)
        .on ("mouseout",mout);

    if (ylabel.toLowerCase().indexOf("percentage") != -1) { var suffix = "%"; }
    else { var suffix = ""; }

    function mover(d) {
      d3.select(this).transition().attr("height", function(d) { return height - y(d.el2) + 5; })
        .attr("y", function(d) { return y(d.el2) - 5;})
        .attr("width", x.rangeBand() + 2)
        .attr("x", function(d) { return x(d.el1) - 1; })
        .style("fill-opacity", .8);
      $(this).attr("rel","twipsy")
        .attr("data-original-title",d.el2 + suffix)
        .twipsy('show');
    }

    function mout(d) {
      d3.select(this).transition().attr("height", function(d) { return height - y(d.el2); })
        .attr("y", function(d) { return y(d.el2); })
        .attr("width", x.rangeBand())
        .attr("x", function(d) { return x(d.el1); })
        .style("fill-opacity", 1);
      $(this).twipsy('hide');
    }

    $("rect[rel=twipsy]").twipsy({ live: true, offset: 4 });

  });
  return bargraph;
}

function verticalBarGraph(pathToCsv,div,w,color,barHeight,barLabelWidth) {
  div = typeof div !== 'undefined' ? div : "body";
  w = typeof w !== 'undefined' ? w : 260;
  color = typeof color !== 'undefined' ? color : "steelblue";
  barHeight = typeof barHeight !== 'undefined' ? barHeight : 18; // height of one bar
  barLabelWidth = typeof barLabelWidth !== 'undefined' ? barLabelWidth : 200; // space reserved for bar labels

  d3.csv(pathToCsv, function(error, data) {
    data.forEach(function(d) {
      d.value = +d.value;
    });

  var valueLabelWidth = 40; // space reserved for value labels (right)
  var barLabelPadding = 5; // padding between bar and bar labels (left)
  var gridLabelHeight = 18; // space reserved for gridline labels
  var gridChartOffset = 3; // space between start of grid and first bar
  var maxBarWidth = w; // width of the bar with the max value

  // accessor functions
  var barLabel = function(d) { return d['name']; };
  var barValue = function(d) { return parseFloat(d['value']); };

  // sorting
  var sortedData = data.sort(function(a, b) {
   return d3.descending(barValue(a), barValue(b));
  });

  // scales
  var yScale = d3.scale.ordinal().domain(d3.range(0, sortedData.length)).rangeBands([0, sortedData.length * barHeight]);
  var y = function(d, i) { return yScale(i); };
  var yText = function(d, i) { return y(d, i) + yScale.rangeBand() / 2; };
  var x = d3.scale.linear().domain([0, d3.max(sortedData, barValue)]).range([0, maxBarWidth]);
  // svg container element
  var chart = d3.select(div).append("svg")
    .attr('width', maxBarWidth + barLabelWidth + valueLabelWidth)
    .attr('height', gridLabelHeight + gridChartOffset + sortedData.length * barHeight);
  // grid line labels
  var gridContainer = chart.append('g')
    .attr('transform', 'translate(' + barLabelWidth + ',' + gridLabelHeight + ')');
  gridContainer.selectAll("text").data(x.ticks(10)).enter().append("text")
    .attr("x", x)
    .attr("dy", -3)
    .attr("text-anchor", "middle")
    .text(String);
  // vertical grid lines
  gridContainer.selectAll("line").data(x.ticks(10)).enter().append("line")
    .attr("x1", x)
    .attr("x2", x)
    .attr("y1", 0)
    .attr("y2", yScale.rangeExtent()[1] + gridChartOffset)
    .style("stroke", "#ccc");
  // bar labels
  var labelsContainer = chart.append('g')
    .attr('transform', 'translate(' + (barLabelWidth - barLabelPadding) + ',' + (gridLabelHeight + gridChartOffset) + ')');
  labelsContainer.selectAll('text').data(sortedData).enter().append('text')
    .attr('y', yText)
    .attr('stroke', 'none')
    .attr('fill', 'black')
    .attr("dy", ".35em") // vertical-align: middle
    .attr('text-anchor', 'end')
    .text(barLabel)
    .style("font-size","13px");
  // bars
  var barsContainer = chart.append('g')
    .attr('transform', 'translate(' + barLabelWidth + ',' + (gridLabelHeight + gridChartOffset) + ')');
  barsContainer.selectAll("rect").data(sortedData).enter().append("rect")
    .attr("y", y)
    .attr("height", yScale.rangeBand())
    .attr("width", function(d) { return x(barValue(d)); })
    .attr("stroke", 'white')
    .attr("fill", color)
    .on ("mouseover",mover)
    .on ("mouseout",mout);

    // .on("mouseover", function(d) { d3.select(this).style("fill-opacity", 0.8); })
    // .on("mouseout", function(d) { d3.select(this).style("fill-opacity", 1); });

  function mover(d) {
    d3.select(this).transition().attr("width", function(d) { return x(barValue(d)) + 3; })
      .style("fill-opacity", .8);
  }

  function mout(d) {
    d3.select(this).transition().attr("width", function(d) { return x(barValue(d)); })
      .style("fill-opacity", 1);
  }

  // bar value labels
  barsContainer.selectAll("text").data(sortedData).enter().append("text")
    .attr("x", function(d) { return x(barValue(d)); })
    .attr("y", yText)
    .attr("dx", 3) // padding-left
    .attr("dy", ".35em") // vertical-align: middle
    .attr("text-anchor", "start") // text-align: right
    .attr("fill", "black")
    .attr("stroke", "none")
    .text(function(d) { return d3.round(barValue(d), 2); })
    .style("font-size","13px");
  // start line
  barsContainer.append("line")
    .attr("y1", -gridChartOffset)
    .attr("y2", yScale.rangeExtent()[1] + gridChartOffset)
    .style("stroke", "#000");
  });
}