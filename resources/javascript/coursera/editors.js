var margin6 = {top: 20, right: 20, bottom: 30, left: 40},
    width6 = 450 - margin6.left - margin6.right,
    height6 = 240 - margin6.top - margin6.bottom;

var x06 = d3.scale.ordinal()
    .rangeRoundBands([0, width6], .1);

var x6 = d3.scale.ordinal();

var y6 = d3.scale.linear()
    .range([height6, 0]);

var color6 = d3.scale.ordinal()
    .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

var xAxis6 = d3.svg.axis()
    .scale(x06)
    .orient("bottom");

var yAxis6 = d3.svg.axis()
    .scale(y6)
    .orient("left")
    .tickFormat(d3.format(".2s"));

var svg6 = d3.select("#editors").append("svg")
    .attr("width", width6 + margin6.left + margin6.right)
    .attr("height", height6 + margin6.top + margin6.bottom)
  .append("g")
    .attr("transform", "translate(" + margin6.left + "," + margin6.top + ")");

d3.csv("../resources/dat/editors.csv", function(error, data) {
  var expLevels = d3.keys(data[0]).filter(function(key) { return key !== "Editor"; });

  data.forEach(function(d) {
    d.exp = expLevels.map(function(name) { return {name: name, value: +d[name]}; });
  });

  x06.domain(data.map(function(d) { return d.Editor; }));
  x6.domain(expLevels).rangeRoundBands([0, x06.rangeBand()]);
  y6.domain([0, d3.max(data, function(d) { return d3.max(d.exp, function(d) { return d.value; }); })]);

  svg6.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height6 + ")")
      .call(xAxis6);

  svg6.append("g")
      .attr("class", "y axis")
      .call(yAxis6)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Percentage")
      .style("font-weight", "bold");

  var field = svg6.selectAll(".editor")
      .data(data)
    .enter().append("g")
      .attr("class", "g")
      .attr("transform", function(d) { return "translate(" + x06(d.Editor) + ",0)"; })
      .style("font-size", "10px");

  field.selectAll("rect")
      .data(function(d) { return d.exp; })
    .enter().append("rect")
      .attr("width", x6.rangeBand())
      .attr("x", function(d) { return x6(d.name); })
      .attr("y", function(d) { return y6(d.value); })
      .attr("height", function(d) { return height6 - y6(d.value); })
      .style("fill", function(d) { return color6(d.name); });

  var legend = svg6.selectAll(".legend")
      .data(expLevels.slice())
    .enter().append("g")
      .attr("class", "legend")
      .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

  legend.append("rect")
      .attr("x", width6 - 18)
      .attr("width", 18)
      .attr("height", 18)
      .style("fill", color6);

  legend.append("text")
      .attr("x", width6 - 24)
      .attr("y", 9)
      .attr("dy", ".35em")
      .style("text-anchor", "end")
      .text(function(d) { return d; });

});