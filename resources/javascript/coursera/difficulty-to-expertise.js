var margin1 = {top: 20, right: 20, bottom: 30, left: 40},
    width1 = 450 - margin1.left - margin1.right,
    height1 = 240 - margin1.top - margin1.bottom;

var x01 = d3.scale.ordinal()
    .rangeRoundBands([0, width1], .1);

var x1 = d3.scale.ordinal();

var y1 = d3.scale.linear()
    .range([height1, 0]);

var color1 = d3.scale.ordinal()
    .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

var xAxis1 = d3.svg.axis()
    .scale(x01)
    .orient("bottom");

var yAxis1 = d3.svg.axis()
    .scale(y1)
    .orient("left")
    .tickFormat(d3.format(".2s"));

var svg1 = d3.select("#difficulty-to-expertise").append("svg")
    .attr("width", width1 + margin1.left + margin1.right)
    .attr("height", height1 + margin1.top + margin1.bottom)
  .append("g")
    .attr("transform", "translate(" + margin1.left + "," + margin1.top + ")");

d3.csv("../resources/dat/difficulty-to-expertise.csv", function(error, data) {
  var expLevels = d3.keys(data[0]).filter(function(key) { return key !== "Experience"; });

  data.forEach(function(d) {
    d.exp = expLevels.map(function(name) { return {name: name, value: +d[name]}; });
  });

  x01.domain(data.map(function(d) { return d.Experience; }));
  x1.domain(expLevels).rangeRoundBands([0, x01.rangeBand()]);
  y1.domain([0, d3.max(data, function(d) { return d3.max(d.exp, function(d) { return d.value; }); })]);

  svg1.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height1 + ")")
      .call(xAxis1);

  svg1.append("g")
      .attr("class", "y axis")
      .call(yAxis1)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Percentage")
      .style("font-weight", "bold");

  var language = svg1.selectAll(".language")
      .data(data)
    .enter().append("g")
      .attr("class", "g")
      .attr("transform", function(d) { return "translate(" + x01(d.Experience) + ",0)"; })
      .style("font-size", "10px");

  language.selectAll("rect")
      .data(function(d) { return d.exp; })
    .enter().append("rect")
      .attr("width", x1.rangeBand())
      .attr("x", function(d) { return x1(d.name); })
      .attr("y", function(d) { return y1(d.value); })
      .attr("height", function(d) { return height1 - y1(d.value); })
      .style("fill", function(d) { return color1(d.name); });

  var legend = svg1.selectAll(".legend")
      .data(expLevels.slice())
    .enter().append("g")
      .attr("class", "legend")
      .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

  legend.append("rect")
      .attr("x", width1 - 18)
      .attr("width", 18)
      .attr("height", 18)
      .style("fill", color1);

  legend.append("text")
      .attr("x", width1 - 24)
      .attr("y", 9)
      .attr("dy", ".35em")
      .style("text-anchor", "end")
      .text(function(d) { return d; });

});