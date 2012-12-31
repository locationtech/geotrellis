var margin4 = {top: 20, right: 20, bottom: 30, left: 40},
    width4 = 450 - margin4.left - margin4.right,
    height4 = 240 - margin4.top - margin4.bottom;

var x04 = d3.scale.ordinal()
    .rangeRoundBands([0, width4], .1);

var x4 = d3.scale.ordinal();

var y4 = d3.scale.linear()
    .range([height4, 0]);

var color4 = d3.scale.ordinal()
    .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

var xAxis4 = d3.svg.axis()
    .scale(x04)
    .orient("bottom");

var yAxis4 = d3.svg.axis()
    .scale(y4)
    .orient("left")
    .tickFormat(d3.format(".2s"));

var svg4 = d3.select("#difficulty-to-field").append("svg")
    .attr("width", width4 + margin4.left + margin4.right)
    .attr("height", height4 + margin4.top + margin4.bottom)
  .append("g")
    .attr("transform", "translate(" + margin4.left + "," + margin4.top + ")");

d3.csv("../resources/dat/difficulty-to-field.csv", function(error, data) {
  var expLevels = d3.keys(data[0]).filter(function(key) { return key !== "Field"; });

  data.forEach(function(d) {
    d.exp = expLevels.map(function(name) { return {name: name, value: +d[name]}; });
  });

  x04.domain(data.map(function(d) { return d.Field; }));
  x4.domain(expLevels).rangeRoundBands([0, x04.rangeBand()]);
  y4.domain([0, d3.max(data, function(d) { return d3.max(d.exp, function(d) { return d.value; }); })]);

  svg4.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height4 + ")")
      .call(xAxis4);

  svg4.append("g")
      .attr("class", "y axis")
      .call(yAxis4)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Percentage")
      .style("font-weight", "bold");

  var field = svg4.selectAll(".field")
      .data(data)
    .enter().append("g")
      .attr("class", "g")
      .attr("transform", function(d) { return "translate(" + x04(d.Field) + ",0)"; })
      .style("font-size", "10px");

  field.selectAll("rect")
      .data(function(d) { return d.exp; })
    .enter().append("rect")
      .attr("width", x4.rangeBand())
      .attr("x", function(d) { return x4(d.name); })
      .attr("y", function(d) { return y4(d.value); })
      .attr("height", function(d) { return height4 - y4(d.value); })
      .style("fill", function(d) { return color4(d.name); });

  var legend = svg4.selectAll(".legend")
      .data(expLevels.slice())
    .enter().append("g")
      .attr("class", "legend")
      .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

  legend.append("rect")
      .attr("x", width4 - 18)
      .attr("width", 18)
      .attr("height", 18)
      .style("fill", color4);

  legend.append("text")
      .attr("x", width4 - 24)
      .attr("y", 9)
      .attr("dy", ".35em")
      .style("text-anchor", "end")
      .text(function(d) { return d; });

});