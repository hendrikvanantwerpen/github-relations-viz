$(document).ready(function(){

    $.ajaxSetup({
        cache: false
    });

    var minTime = maxTime = 0
    var degree = 1

    function createGraph() {
    }

    function updateGraph(nodes,links) {
        // rather crude way to reset the graph for now, updating is nicer
        $( "#graphanchor" ).html( "" )

        var w = 960, h = 500;

        var vis = d3.select("#graphanchor").append("svg:svg").attr("width", w).attr("height", h);

        var labelAnchors = [];
        var labelAnchorLinks = [];

        for(var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            labelAnchors.push({
                node : node
            });
            labelAnchors.push({
                node : node
            });
            labelAnchorLinks.push({
                source : i * 2,
                target : i * 2 + 1,
                weight : 1
            });
        };

        var force = d3.layout.force()
                      .size([w, h])
                      .nodes(nodes)
                      .links(links)
                      .gravity(1)
                      //.linkDistance(50)
                      .linkDistance(function(l,i){ return 200/(l.value*l.value); })
                      .charge(-1000)
                      .linkStrength(10);
                      //.linkStrength(function(x) { return x.value * 10; });

        force.start();

        var force2 = d3.layout.force().nodes(labelAnchors).links(labelAnchorLinks).gravity(0).linkDistance(0).linkStrength(8).charge(-100).size([w, h]);
        force2.start();

        var link = vis.selectAll("line.link").data(links).enter().append("svg:line").attr("class", "link").style("stroke", "#CCC");

        var node = vis.selectAll("g.node").data(force.nodes()).enter().append("svg:g").attr("class", "node");
        node.append("svg:circle").attr("r", 5).style("fill", "#555").style("stroke", "#FFF").style("stroke-width", 3);
        node.call(force.drag);

        var anchorLink = vis.selectAll("line.anchorLink").data(labelAnchorLinks)//.enter().append("svg:line").attr("class", "anchorLink").style("stroke", "#999");

        var anchorNode = vis.selectAll("g.anchorNode").data(force2.nodes()).enter().append("svg:g").attr("class", "anchorNode");
        anchorNode.append("svg:circle").attr("r", 0).style("fill", "#FFF");
            anchorNode.append("svg:text").text(function(d, i) {
            return i % 2 != 0 ? d.node.name : ""
        }).style("fill", "#555").style("font-family", "Arial").style("font-size", 12);

        var updateLink = function() {
            this.attr("x1", function(d) {
                return d.source.x;
            }).attr("y1", function(d) {
                return d.source.y;
            }).attr("x2", function(d) {
                return d.target.x;
            }).attr("y2", function(d) {
                return d.target.y;
            });

        }

        var updateNode = function() {
            this.attr("transform", function(d) {
                return "translate(" + d.x + "," + d.y + ")";
            });

        }


        force.on("tick", function() {

            force2.start();

            node.call(updateNode);

            anchorNode.each(function(d, i) {
                if(i % 2 == 0) {
                    d.x = d.node.x;
                    d.y = d.node.y;
                } else {
                    var b = this.childNodes[1].getBBox();

                    var diffX = d.x - d.node.x;
                    var diffY = d.y - d.node.y;

                    var dist = Math.sqrt(diffX * diffX + diffY * diffY);

                    var shiftX = b.width * (diffX - dist) / (dist * 2);
                    shiftX = Math.max(-b.width, Math.min(0, shiftX));
                    var shiftY = 5;
                    this.childNodes[1].setAttribute("transform", "translate(" + shiftX + "," + shiftY + ")");
                }
            });


            anchorNode.call(updateNode);

            link.call(updateLink);
            anchorLink.call(updateLink);

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
          return new Date(1000*e).toString();
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
