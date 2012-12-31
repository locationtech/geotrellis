  $('#map-population').vectorMap({
    map: 'world_mill_en',
  regionStyle: {
    hover: {
      "fill-opacity": 0.6
    }
  },
  series: {
    regions: [{
      values: studentData,
      scale: ['#ffffff', '#005f8a'],
      normalizeFunction: 'polynomial',
    }]
  },
  onRegionLabelShow: function(e, el, code){
    el.html("<span style='font-size: 16px'><b>"+el.html()+"</b></span><br>Number of Students: <b>"+studentData[code]+"</b><br><span style='opacity: 0.5;'>Percentage: "+((studentData[code]/tot)*100).toPrecision(3)+"%</span>");
  }
});