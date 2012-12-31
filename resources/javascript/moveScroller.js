function moveScroller() {
  var a = function() {
    if($("#scroller-anchor").length == 0) { return; }
    var b = $(window).scrollTop();
    var d = $("#scroller-anchor").offset().top;
    var c=$("#scroller");
    if (b>d) {
      c.css({position:"fixed",top:"124px"})
    } else {
      if (b<=d) {
        c.css({position:"relative",top:""})
      }
    }
  };
  $(window).scroll(a);a()
}