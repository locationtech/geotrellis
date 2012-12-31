var margin3 = {top: 20, right: 20, bottom: 30, left: 40},
    width3 = 960 - margin3.left - margin3.right,
    height3 = 480 - margin3.top - margin3.bottom;

var x03 = d3.scale.ordinal()
    .rangeRoundBands([0, width3], .1);

var x13 = d3.scale.ordinal();

var y3 = d3.scale.linear()
    .range([height3, 0]);

var color3 = d3.scale.ordinal()
    .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

var xAxis3 = d3.svg.axis()
    .scale(x03)
    .orient("bottom");

var yAxis3 = d3.svg.axis()
    .scale(y3)
    .orient("left")
    .tickFormat(d3.format(".2s"));

var svg3 = d3.select("#languages-percentages").append("svg")
    .attr("width", width3 + margin3.left + margin3.right)
    .attr("height", height3 + margin3.top + margin3.bottom)
  .append("g")
    .attr("transform", "translate(" + margin3.left + "," + margin3.top + ")");

d3.csv("../resources/dat/languages-percentages.csv", function(error, data) {
  var expLevels = d3.keys(data[0]).filter(function(key) { return key !== "Language"; });

  data.forEach(function(d) {
    d.exp = expLevels.map(function(name) { return {name: name, value: +d[name]}; });
  });

  x03.domain(data.map(function(d) { return d.Language; }));
  x13.domain(expLevels).rangeRoundBands([0, x03.rangeBand()]);
  y3.domain([0, d3.max(data, function(d) { return d3.max(d.exp, function(d) { return d.value; }); })]);

  svg3.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height3 + ")")
      .call(xAxis3);

  svg3.append("g")
      .attr("class", "y axis")
      .call(yAxis3)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Percentage")
      .style("font-weight", "bold");

  var language = svg3.selectAll(".language")
      .data(data)
    .enter().append("g")
      .attr("class", "g")
      .attr("transform", function(d) { return "translate(" + x03(d.Language) + ",0)"; })
      .style("font-size", "10px");

  language.selectAll("rect")
      .data(function(d) { return d.exp; })
    .enter().append("rect")
      .attr("width", x13.rangeBand())
      .attr("x", function(d) { return x13(d.name); })
      .attr("y", function(d) { return y3(d.value); })
      .attr("height", function(d) { return height3 - y3(d.value); })
      .style("fill", function(d) { return color3(d.name); });

  var legend = svg3.selectAll(".legend")
      .data(expLevels.slice())
    .enter().append("g")
      .attr("class", "legend")
      .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

  legend.append("rect")
      .attr("x", width3 - 18)
      .attr("width", 18)
      .attr("height", 18)
      .style("fill", color3);

  legend.append("text")
      .attr("x", width3 - 24)
      .attr("y", 9)
      .attr("dy", ".35em")
      .style("text-anchor", "end")
      .text(function(d) { return d; });

});