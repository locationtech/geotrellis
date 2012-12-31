var r1 = Raphael("where-apply",500, 240);
var pc1 = r1.piechart(110, 120, 100, [2533, 1186, 1837, 302, 1634],{ legend: ["%%.%% Personal Projects", "%%.%% Individual project at work", "%%.%% Team project at work", "%%.%% University projects", "%%.%% No application plans, general interest"], legendpos: "east" });

pc1.hover(function () {
    this.sector.stop();
    this.sector.scale(1.1, 1.1, this.cx, this.cy);

    if (this.label) {
        this.label[0].stop();
        this.label[0].attr({ r: 7.5 });
        this.label[1].attr({ "font-weight": 800 });
    }
}, function () {
    this.sector.animate({ transform: 's1 1 ' + this.cx + ' ' + this.cy }, 500, "bounce");

    if (this.label) {
        this.label[0].animate({ r: 5 }, 500, "bounce");
        this.label[1].attr({ "font-weight": 400 });
    }
});
