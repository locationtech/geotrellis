  $('#map-density').vectorMap({
  map: 'world_mill_en',
  regionStyle: {
    hover: {
      "fill-opacity": 0.6
    }
  },
  series: {
    regions: [{
      values: density,
      scale: ['#ffffff', '#330066'],
      normalizeFunction: 'polynomial',
    }]
  },
  onRegionLabelShow: function(e, el, code){
    el.html("<span style='font-size: 16px'><b>"+el.html()+"</b></span><br>Number of Students: <b>"+count[code]+"</b><br><span style='opacity: 0.5;'>Population: "+population[code]+"</span>");
  }
});