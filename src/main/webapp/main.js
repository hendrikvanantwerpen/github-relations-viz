$(document).ready(function(){

    $.ajaxSetup({
        cache: false
    });

    var minTime = maxTime = 0
    var degree = 1

    d3.fisheye = function() {
        var radius = 200,
            power = 2,
            k0,
            k1,
            center = [0, 0];
  
      function fisheye(d) {
          var dx = d.x - center[0],
              dy = d.y - center[1],
              dd = Math.sqrt(dx * dx + dy * dy);
          if (dd >= radius) return {x: d.x, y: d.y, z: 1};
          var k = k0 * (1 - Math.exp(-dd * k1)) / dd * .75 + .25;
          return {x: center[0] + dx * k, y: center[1] + dy * k, z: Math.min(k, 10)};
      }
  
      function rescale() {
          k0 = Math.exp(power);
          k0 = k0 / (k0 - 1) * radius;
          k1 = power / radius;
          return fisheye;
      }
  
      fisheye.radius = function(_) {
          if (!arguments.length) return radius;
          radius = +_;
          return rescale();
      };
  
      fisheye.power = function(_) {
          if (!arguments.length) return power;
          power = +_;
          return rescale();
      };
  
      fisheye.center = function(_) {
          if (!arguments.length) return center;
          center = _;
          return fisheye;
      };
  
      return rescale();
    };

    function updateGraph(nodes,links) {
        $( "#graph" ).html( "" )

        var w = $( "#graph" ).width(),
            h = $( "#graph" ).height(),
            fill = d3.scale.category20(),
            trans = [0,0]
            scale = 1;

        var fisheye = d3.fisheye()
                        .radius(100)
                        .power(3);

        var vis = d3.select("#graph")
                    .append("svg:svg")
                    .attr("width", w)
                    .attr("height", h)
                    .attr("pointer-events", "all")
                    .append('svg:g')
                    .call(d3.behavior.zoom().on("zoom", onzoom ))
                    .append('svg:g');

        var background = vis.append('svg:rect')
                            .attr('width', w)
                            .attr('height', h)
                            .attr('fill', 'white');

        function onzoom() {
            scale = d3.event.scale
            trans = d3.event.translate
            vis.attr("transform","translate("+trans+")"+" scale("+scale+")");
            background.attr("transform","translate("+[-trans[0]/scale,-trans[1]/scale]+") scale("+1/scale+")");
        }

        var force = d3.layout.force()
                      .size([w, h])
                      .nodes(nodes)
                      .links(links)
                      .charge(-1000)
                      .linkDistance(function(d) { return 100/d.value; })
                      .linkStrength(function(d){ return 0.2; });

        var link = vis.selectAll("line.link")
                      .data(links)
                      .enter()
                      .append("svg:line")
                      .attr("class", "link")
                      .style("stroke-width", function(d) { return Math.sqrt(d.value); })
                      .style("stroke", "#CCC")
                      .attr("x1", function(d) { return d.source.x; })
                      .attr("y1", function(d) { return d.source.y; })
                      .attr("x2", function(d) { return d.target.x; })
                      .attr("y2", function(d) { return d.target.y; })
                      .on("mouseover", showLinkPopup)
                      .on("mouseout", hidePopup);

        var node = vis.selectAll("circle.node")
                      .data(nodes)
                      .enter()
                      .append("svg:circle")
                      .attr("class", "node")
                      .attr("cx", function(d) { return d.x; })
                      .attr("cy", function(d) { return d.y; })
                      .attr("r", function(d) { return d.weight; })
                      .on("mouseover", showNodePopup)
                      .on("mouseout", hidePopup);

        vis.style("opacity", 1e-6)
           .transition()
           .duration(1000)
           .style("opacity", 1);

        force.start();
        var n = nodes.length;
        for (var i = 10*n; i > 0; --i) force.tick();
        force.stop();

        function showLinkPopup(d) {
            var x = d3.event.x
            var y = d3.event.y
            $("#pop-up").fadeOut(100,function () {
                // Popup content
                $("#pop-up-title").html("Link");
                $("#pop-img").html(d.value);
                $("#pop-desc").html("contributors");
                // Popup position
                var popLeft = x+20;
                var popTop = y+20;
                $("#pop-up").css({"left":popLeft,"top":popTop});
                $("#pop-up").fadeIn(100);
            });
        }

        function showNodePopup(d) {
            $("#pop-up").fadeOut(100,function () {
                // Popup content
                $("#pop-up-title").html("Project "+d.name);
                $("#pop-img").html(d.weight);
                $("#pop-desc").html("connected projects");
                // Popup position
                var popLeft = (d.x*scale)+trans[0]+20;//lE.cL[0] + 20;
                var popTop = (d.y*scale)+trans[1]+20;//lE.cL[1] + 70;
                $("#pop-up").css({"left":popLeft,"top":popTop});
                $("#pop-up").fadeIn(100);
            });
        }

        function hidePopup(d) {
            $("#pop-up").fadeOut(50);
            d3.select(this).attr("fill","url(#ten1)");
        }

        vis.on("mousemove", function() {

            fisheye.center(d3.mouse(this));

            node.each(function(d) { d.display = fisheye(d); })
                .attr("cx", function(d) { return d.display.x; })
                .attr("cy", function(d) { return d.display.y; })
                .attr("r", function(d) { return d.display.z * d.weight; });

            link.attr("x1", function(d) { return d.source.display.x; })
                .attr("y1", function(d) { return d.source.display.y; })
                .attr("x2", function(d) { return d.target.display.x; })
                .attr("y2", function(d) { return d.target.display.y; });
        });

    }

  function createUI(minDate,maxDate) {
    $( "#date-range-slider" ).slider({
      range: true,
      min: minDate,
      max: maxDate,
      values: [ minTime, maxTime ],
      slide: function( event, ui ) {
          minTime = ui.values[ 0 ];
          maxTime = ui.values[ 1 ];
          updateUI();
      }
    });
    
    $( "#degree-range-slider" ).slider({
      range: false,
      min: 1,
      max: 20,
      values: [ degree ],
      slide: function( event, ui ) {
          degree = ui.values[0];
          updateUI();
      }
    });
    
    $( "#refresh" ).button()
    .click(function(e){
      refreshGraph();
      e.preventDefault();
    });
  }

  
  function updateUI() {
      function fmtEpoch(e) {
          var day = 24 * 3600;
          var re = e - (e % day);
          var d = new Date(1000*re);
          return d.getFullYear()+"-"+(d.getMonth()+1)+"-"+d.getDate();
      }
      $( "#date-from" ).text( fmtEpoch(minTime) );
      $( "#date-to" ).text( fmtEpoch(maxTime) );
      $('#degree').text( degree );
  }

  function updateStatus(msg) {
      $("#statusLabel").text(msg);
  }

  var request = null;
  function refreshGraph() {
      if(request !== null) {
          updateStatus("try again later - request in progress");
      } else {
        updateStatus('loading...');
        request = $.getJSON('/d3data?from='+minTime+'&to='+maxTime+'&degree='+degree)
        .success(function(json){
          request = null;
          if ( !$.isEmptyObject(json) ) {
            updateStatus('loading json into graph');
            updateGraph(json.nodes,json.links);
            updateStatus('graph rendered - ready');
          } else {
            updateStatus('empty graph - ready');
          }
        })
        .error(function(){
         updateStatus('error');
         request = null;
       });
     }
  }

  updateStatus('getting date range');
  $.getJSON('/range')
  .success(function(range){
    minTime = maxTime = range.min
    createUI(range.min,range.max);
    updateStatus('ready');
  });

});
