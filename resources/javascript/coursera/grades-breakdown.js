var margin2 = {top: 20, right: 20, bottom: 30, left: 40},
    width2 = 960 - margin2.left - margin2.right,
    height2 = 480 - margin2.top - margin2.bottom;

var x2 = d3.scale.ordinal()
    .rangeRoundBands([0, width2], .1);

var y2 = d3.scale.linear()
    .range([height2, 0]);

var xAxis2 = d3.svg.axis()
    .scale(x2)
    .orient("bottom");

var yAxis2 = d3.svg.axis()
    .scale(y2)
    .orient("left");

var svg2 = d3.select("#grades-breakdown").append("svg")
    .attr("width", width2 + margin2.left + margin2.right)
    .attr("height", height2 + margin2.top + margin2.bottom)
  .append("g")
    .attr("transform", "translate(" + margin2.left + "," + margin2.top + ")");

d3.csv("../resources/dat/grades-breakdown.csv", function(error, data) {

  data.forEach(function(d) {
    d.grade = +d.grade;
    d.count = +d.count;
  });

  x2.domain(data.map(function(d) { return d.grade; }));
  y2.domain([0, d3.max(data, function(d) { return d.count; })]);

  svg2.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height2 + ")")
      .style("font-size", "9px")
      .call(xAxis2)
      .append("text")
      .attr("y", 30)
      .attr("x", width2 - margin2.left)
      .style("font-weight", "bold")
      .style("font-size", "13px")
      .text("Score");

  svg2.append("g")
      .attr("class", "y axis")
      .call(yAxis2)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .style("font-weight", "bold")
      .text("Number of Students");

  svg2.selectAll(".bar")
      .data(data)
    .enter().append("rect")
      .attr("class", "bar")
      .attr("x", function(d) { return x2(d.grade); })
      .attr("width", x2.rangeBand())
      .attr("y", function(d) { return y2(d.count); })
      .attr("height", function(d) { return height2 - y2(d.count); })
      .on ("mouseover",mover)
      .on ("mouseout",mout);

    function mover(d) {
        $("#pop-up").fadeOut(0,function () {
            // Popup content
            $("#pop-up-title").html(d.count);

            // Popup position
            var popLeft = x2(d.grade) + margin2.left;//lE.cL[0] + 20;
            var popTop = y2(d.count);//lE.cL[1] + 70;
            $("#pop-up").css({"left":popLeft,"top":popTop});
            $("#pop-up").fadeIn(0);
        });
        d3.select(this).style("fill", "#3A6991");
    }

    function mout(d) {
        $("#pop-up").fadeOut(50);
        d3.select(this).attr("fill","url(#ten1)");
        d3.select(this).style("fill", "steelblue");
    }
});