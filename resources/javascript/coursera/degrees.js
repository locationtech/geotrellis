var margin5 = {top: 20, right: 20, bottom: 60, left: 40},
    width5 = 450 - margin5.left - margin5.right,
    height5 = 240 - margin5.top - margin5.bottom;

// var formatPercent = d3.format(".0%");

var x5 = d3.scale.ordinal()
    .rangeRoundBands([0, width5], .1);

var y5 = d3.scale.linear()
    .range([height5, 0]);

var xAxis5 = d3.svg.axis()
    .scale(x5)
    .orient("bottom");

var yAxis5 = d3.svg.axis()
    .scale(y5)
    .orient("left")
    .tickFormat(d3.format(".2s"));

var svg5 = d3.select("#degrees").append("svg")
    .attr("width", width5 + margin5.left + margin5.right)
    .attr("height", height5 + margin5.top + margin5.bottom)
  .append("g")
    .attr("transform", "translate(" + margin5.left + "," + margin5.top + ")");

d3.csv("../resources/dat/degrees.csv", function(error, data) {

  data.forEach(function(d) {
    d.count = +d.count;
  });

  x5.domain(data.map(function(d) { return d.degree; }));
  y5.domain([0, d3.max(data, function(d) { return d.count; })]);

  var xax = svg5.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height5 + ")")
      .call(xAxis5);

  xax.selectAll("text")
  .attr("text-anchor", function(d) { return "end" })
  .attr("transform", function(d) { return "translate(-20,20)rotate(-45)"; });

  svg5.append("g")
      .attr("class", "y axis")
      .call(yAxis5)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Percentage")
      .style("font-weight","bold");

  svg5.selectAll(".bar")
      .data(data)
    .enter().append("rect")
      .attr("class", "bar")
      .attr("x", function(d) { return x5(d.degree); })
      .attr("width", x5.rangeBand())
      .attr("y", function(d) { return y5(d.count); })
      .attr("height", function(d) { return height5 - y5(d.count); });

});
