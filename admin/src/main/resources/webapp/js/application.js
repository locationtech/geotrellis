var gtUrl = function(path) {
    return ("/gt/" + path).replace("//","/");
}

var getLayer = function(url,attrib) {
    return L.tileLayer(url, { maxZoom: 18, attribution: attrib });
};

var Layers = {
    stamen: { 
        toner:  'http://{s}.tile.stamen.com/toner/{z}/{x}/{y}.png',   
        terrain: 'http://{s}.tile.stamen.com/terrain/{z}/{x}/{y}.png',
        watercolor: 'http://{s}.tile.stamen.com/watercolor/{z}/{x}/{y}.png',
        attrib: 'Map data &copy;2013 OpenStreetMap contributors, Tiles &copy;2013 Stamen Design'
    },
    mapBox: {
        azavea:     'http://{s}.tiles.mapbox.com/v3/azavea.map-zbompf85/{z}/{x}/{y}.png',
        worldGlass:     'http://{s}.tiles.mapbox.com/v3/mapbox.world-glass/{z}/{x}/{y}.png',
        worldBlank:  'http://{s}.tiles.mapbox.com/v3/mapbox.world-blank-light/{z}/{x}/{y}.png',
        worldLight: 'http://{s}.tiles.mapbox.com/v3/mapbox.world-light/{z}/{x}/{y}.png',
        attrib: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://mapbox.com">MapBox</a>'
    }
};

var map = (function() {
    var selected = getLayer(Layers.mapBox.azavea,Layers.mapBox.attrib);
    var baseLayers = {
        "Azavea" : selected,
        "World Light" : getLayer(Layers.mapBox.worldLight,Layers.mapBox.attrib),
        "Terrain" : getLayer(Layers.stamen.terrain,Layers.stamen.attrib),
        "Watercolor" : getLayer(Layers.stamen.watercolor,Layers.stamen.attrib),
        "Toner" : getLayer(Layers.stamen.toner,Layers.stamen.attrib),
        "Glass" : getLayer(Layers.mapBox.worldGlass,Layers.mapBox.attrib),
        "Blank" : getLayer(Layers.mapBox.worldBlank,Layers.mapBox.attrib)
    };

    var m = L.map('map').setView([39.9886950160466,-75.1519775390625], 10);

    selected.addTo(m);

    m.lc = L.control.layers(baseLayers).addTo(m);

    $('#map').resize(function() {
        m.setView(m.getBounds(),m.getZoom());
    });

    return m;
})()

var valueViewer = (function() {
    var createValueGrid = function (rows, cols, values){
        var i=0;
        var grid = document.createElement('table');
        var centerRow = Math.floor(rows / 2);
        var centerCol = Math.floor(cols / 2);
        grid.className = 'valueGrid';
        grid.id = 'valueGrid';
        for (var r=0;r<rows;++r){
            var tr = grid.appendChild(document.createElement('tr'));
            for (var c=0;c<cols;++c){
                var cell = tr.appendChild(document.createElement('td'));
                cell.innerHTML = values[r*cols + c];
                if(r == centerRow && c == centerCol) {
                    cell.className = 'centerCell';
                }
            }
        }
        return grid;
    };
    return {
        update : function(latlng) {
            $.ajax({
                url: gtUrl('layer/valuegrid'),
                data: { 'store' : layerViewer.getLayer().store,
                        'layer' : layerViewer.getLayer().name,
                        'lat': latlng.lat,
                        'lng': latlng.lng },
                dataType: "json",
                success: function(data) {
                    if(data.success) {
                        var d  = Math.floor(Math.sqrt(data.values.length))
                        var grid = createValueGrid(d,d,data.values);
                        $('#valueViewer').html("");
                        $('#valueViewer').append(grid);
                        valueViewer.updateSize();
                    } else {
                        // Display no success in div.
                        $('#valueViewer').html("");
                        $('#valueViewer').append(
                            $('<span class="label label-info">Could not get raster values. Is the Raster projection Web Mercator?</span>'));
                    }
                }
            });
        },
        updateSize : function() {
            if($('#valueGrid')) {
                $('#valueGrid').css({'height': ($('#valueGrid').width()) +'px'});
            }
        }
    };
})();

var layerViewer = (function() {
    var layer = null;

    var breaks = null;
    var mapLayer = null;
    var opacity = 0.5;
    var colorRamp = "blue-to-red";
    var numBreaks = 10;

    update = function() {
        $.ajax({
            url: gtUrl('layer/breaks'),
            data: { 'store' : layer.store,
                    'layer' : layer.name,
                    'numBreaks': numBreaks },
            dataType: "json",
            success: function(r) {
                breaks = r.classBreaks;

                if (mapLayer) {
                    map.lc.removeLayer(mapLayer);
                    map.removeLayer(mapLayer);
                } else { 
                    // First time loading layer, set up the value viewer.
                    $('#info-tab-link').show();
                    map.on('click', function(e) {
                        valueViewer.update(e.latlng);
                        $('a[href=#layer-info]').tab('show');
                    });
                }

                mapLayer = new L.TileLayer.WMS(gtUrl("layer/render"), {
                    store: layer.store,
                    layer: layer.name,
                    format: 'image/png',
                    breaks: breaks,
                    transparent: true,
                    colorRamp: colorRamp,                    
                })

                mapLayer.setOpacity(opacity);
                mapLayer.addTo(map);
                map.lc.addOverlay(mapLayer, layer.name);

            }
        });

    };

    // Opacity
    var opacitySlider = $("#opacity-slider").slider({
        value: opacity,
        min: 0,
        max: 1,
        step: .02,
        slide: function( event, ui ) {
          opacity = ui.value;
          mapLayer.setOpacity(opacity);
        }
    });

    return {
        getLayer: function() { return layer; },
        
        setLayer: function(ls) { 
            layer = ls; 
            if(!layer) { return; }
            
            var latlong = layer.rasterExtent.latlong
            var southWest = new L.LatLng(latlong.latmin, latlong.longmin);
            var northEast = new L.LatLng(latlong.latmax, latlong.longmax);
            var bounds = new L.LatLngBounds(southWest, northEast);
            if(!bounds.contains(map.getBounds())) {
                map.fitBounds(bounds);
            }

            update(); 
        },
        setNumBreaks: function(nb) {
            numBreaks = nb;
            update();
        },
        setOpacity: function(o) {
            opacity = o;
            opacitySlider.slider('value', o);
        },
        setColorRamp: function(key) { 
            colorRamp = key;
            update();
        },
        getColorRamp: function(key) { 
            return colorRamp;
        },
        update: update,

        getMapLayer: function() { return mapLayer; }
    };

})();

var layerInfo = (function() {
    var layer = null;

    var addInfo = function(infoData,name,value) {
        infoData.append($('<dt>' + name + '</dt>'));
        infoData.append($('<dd>' + value + '</dd>'));
    }

    var update = function() {
        if(layer != null) {
            var infoData = $("#layer-info-data");
            infoData.empty();

            $("#layer-info-name").text(layer.name);

            addInfo(infoData,"Columns",layer.rasterExtent.cols);
            addInfo(infoData,"Rows",layer.rasterExtent.rows);
            addInfo(infoData,"Cell Width",layer.rasterExtent.cellwidth);
            addInfo(infoData,"Cell Height",layer.rasterExtent.cellheight);
            addInfo(infoData,"Data Type",layer.datatype);
        }
    };

    return {
        getLayer: function() { return layer; },
        setLayer: function(l) { 
            layer = l;
            update();
        },
        update: update,
        clear: function() {
            layer = null;
            $('a[href=#browser]').tab('show');
            $("#layer-info-name").text("");
            $("#layer-info-data").empty();
        }
    };
})();

var colorRamps = (function() {
    var makeColorRamp = function(colorDef) {
        var ramps = $("#color-ramp-menu");
        p
        var p = $("#colorRampTemplate").clone();
        p.find('img').attr("src",colorDef.image);
        p.click(function() {
            $("#activeRamp").attr("src",colorDef.image);
            layerViewer.setColorRamp(colorDef.key);
        });
        if(colorDef.key == layerViewer.getColorRamp()) {
            $("#activeRamp").attr("src",colorDef.image);
        }
        p.show();
        ramps.append(p);
    }

    return { 
        bindColorRamps: function() {
            $.ajax({
                url: gtUrl('colors'),
                dataType: 'json',
                success: function(data) {
                    _.map(data.colors, makeColorRamp)
                }
            });
        }
    }
})();
var active = null;

$.getJSON(gtUrl('catalog'), function(catalog) {
    $("#page-title").text("GeoTrellis Viewer - " + catalog.name);

    treeNodes = _.map(catalog.stores,function(store) {
        return {
            title: store.name, 
            isFolder: true,
            key: store.name,
            children: _.map(store.layers, function(layer) {
                return { title: layer, store: store.name }
            })
        }
    });

    if(treeNodes.length == 0) {
        $("#catalog-tree").append($("<span class='emptymsg'>Catalog is empty!</span>"));
    } else {

        $("#catalog-tree").dynatree({
            onActivate: function(node) {
                if(!node.data.isFolder) {
                    $.getJSON(gtUrl("layer/info?layer="+node.data.title+"&store="+node.data.store), function(layer) {
                        layerViewer.setLayer(layer);
                        layerInfo.setLayer(layer);
                    });
                }
            },
            children: treeNodes
        });
    }
});

var setupSize = function() {
    var bottomPadding = 10;

    var resize = function(){
        var pane = $('#left-pane');
        var height = $(window).height() - pane.position().top - bottomPadding;
        pane.css({'height': height +'px'});

        var mapDiv = $('#map');
        var height = $(window).height() - mapDiv.offset().top - bottomPadding;
        mapDiv.css({'height': height +'px'});

        map.invalidateSize();
        valueViewer.updateSize();
    };
    resize();
    $(window).resize(resize);
};

// On page load
$(document).ready(function() {
    colorRamps.bindColorRamps();
    setupSize();
});
