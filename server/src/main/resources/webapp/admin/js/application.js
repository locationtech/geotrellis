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

    var m = L.map('map').setView([34.76192255039478,-85.35140991210938], 9);

    selected.addTo(m);

    m.lc = L.control.layers(baseLayers).addTo(m);

    $('#map').resize(function() {
        m.setView(m.getBounds(),m.getZoom());
    });

    return m;
})()

var layerViewer = (function() {
    var layer = null;

    var breaks = null;
    var mapLayer = null;
    var opacity = 0.5;
    var colorRamp = "blue-to-red";
    var numBreaks = 10;

    var handleLayerClick = function(e) {
        $.ajax({
            url: gtUrl('admin/layer/values'),
            data: { 'layer' : layer.name,
                    'size'  : 5,
                    'lat'   : e.latlng.lat,
                    'lng'   : e.latlng.lng },
            dataType: 'json',
            success: function(d) {
                //var valueGrid = 
                alert(JSON.stringify(d));
            }
        });
    };

    update = function() {
        $.ajax({
            url: gtUrl('admin/breaks'),
            data: { 'layer' : layer.name,
                    'numBreaks': numBreaks },
            dataType: "json",
            success: function(r) {
                breaks = r.classBreaks;

                if (mapLayer) {
                    map.lc.removeLayer(mapLayer);
                    map.removeLayer(mapLayer);
                }

                mapLayer = new L.TileLayer.WMS(gtUrl("admin/layer/render"), {
                    layer: layer.name,
                    format: 'image/png',
                    breaks: breaks,
                    transparent: true,
                    colorRamp: colorRamp,
                    click: handleLayerClick
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
            layerViewer.setColorRamp(colorDef.key);
        });
        p.show();
        ramps.append(p);
    }

    return { 
        bindColorRamps: function() {
            $.ajax({
                url: gtUrl('admin/colors'),
                dataType: 'json',
                success: function(data) {
                    _.map(data.colors, makeColorRamp)
                }
            });
        }
    }
})();
var active = null;

    $.getJSON(gtUrl('admin/catalog'), function(catalog) {
    $("#page-title").text("GeoTrellis Viewer - " + catalog.name);

    treeNodes = _.map(catalog.stores,function(store) {
        return {
            title: store.name, 
            isFolder: true,
            key: store.name,
            children: _.map(store.layers, function(layer) {
                return { title: layer }
            })
        }
    });

    $("#catalog-tree").dynatree({
        onActivate: function(node) {
            if(!node.data.isFolder) {
                $.getJSON(gtUrl("admin/layer/info?layer="+node.data.title), function(layer) {
                    layerViewer.setLayer(layer);
                    layerInfo.setLayer(layer);
                });
            }
        },
        children: treeNodes
    });
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
    };
    resize();
    $(window).resize(resize);
};

// On page load
$(document).ready(function() {
    colorRamps.bindColorRamps();
    setupSize();
});
