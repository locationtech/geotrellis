var r2 = Raphael("what-interested-you",420, 240);
var pc2 = r2.piechart(310, 120, 100, [4810, 194, 2488],{ legend: ["%%.%% Personal Interest/Curiosity", "%%.%% University Studies", "%%.%% Helps With Profession"], legendpos: "west" });

pc2.hover(function () {
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