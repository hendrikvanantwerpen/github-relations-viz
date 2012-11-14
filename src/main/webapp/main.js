$(document).ready(function(){

    var UNKNOWN = '[unknown]'

    var minTime = maxTime = 0;
    var stepTime = 1;
    var minLinkValue = 1;
    var selectedlangs = {};
    var nodes = [];
    var links = [];        
        
    $.ajaxSetup({
        cache: false
    });

    ///////////////////////////////////////
    // D3 Fisheye
    
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

    ///////////////////////////////////////
    // D3 Graph
    
    var graphWidth = $( "#graph" ).width(),
        graphHeight = $( "#graph" ).height(),
        graphTrans = [0,0],
        graphScale = 1;
    
    var langcolor = d3.scale.category20();
    var langcolormap = {}
    var langbtnmap = {}
    var langcntmap = {}
    function getLangColor(lang) {
        var c = langcolormap[lang];
        if ( !c ) {
            c = langcolor(lang);
            langcolormap[lang] = c;
            selectedlangs[lang] = true;
            var btn = $('<div/>',{
                class: 'langButton',
                style: 'background: '+c+';'
            }).button({
                "label": lang || UNKNOWN
            }).appendTo( "#languages" )[0];
            langbtnmap[lang] = btn;
            $( btn ).click(function(e){
                if ( selectedlangs[lang] ) {
                    $( btn ).css( "background", "inherit" );
                    selectedlangs[lang] = false;
                } else {
                    $( btn ).css( "background", c );
                    selectedlangs[lang] = true;
                }
                updateGraph();
            });
        }
        return c;
    }
    function setLangCount(lang,count) {
        langcntmap[lang] = count;
        $( langbtnmap[lang] ).button( "option", "label", (lang || UNKNOWN) + " (0/" + count + ")" );
    }
    function setLangViewCount(lang,viewcount) {
        var count = langcntmap[lang];
        $( langbtnmap[lang] ).button( "option", "label", (lang || UNKNOWN) + " ("+viewcount+"/" + count + ")" );
    }
    function resetLangCount() {
        for (var lang in langcntmap) {
            setLangCount(lang,0)
        }
    }

    var fisheye = d3.fisheye()
                    .radius(100)
                    .power(3);

    var vis = d3.select("#graph")
                .append("svg:svg")
                .attr("width", graphWidth)
                .attr("height", graphHeight)
                .attr("pointer-events", "all")
                .append('svg:g')
                .call(d3.behavior.zoom().on("zoom", onzoom ))
                .append('svg:g');
    
    var background = vis.append('svg:rect')
                        .attr('width', graphWidth)
                        .attr('height', graphHeight)
                        .attr('fill', 'white');

    function onzoom() {
        scale = d3.event.scale
        graphTrans = d3.event.translate
        vis.attr("transform","translate("+graphTrans+")"+" scale("+graphScale+")");
        background.attr("transform","translate("+[-graphTrans[0]/graphScale,-graphTrans[1]/graphScale]+") scale("+1/graphScale+")");
    }

    var force = d3.layout.force()
                  .size([graphWidth, graphHeight])
                  .charge(-500)
                  .linkDistance(function(link){
                      var d = link.source.value+link.target.value;
                      return 3*d; })
                  .linkStrength( 0.1 );

    function updateData(ns,ls) {
        updateStatus("updating data")
        var nodeIndex = {};
        var langHist = {};
        
        var n;
        for (var ni = ns.length-1; ni >=0; ni-- ) {
            n = ns[ni];
            nodeIndex[n.id] = n;
            langHist[n.lang] = (langHist[n.lang] || 0) + 1
        }

        resetLangCount();
        for (var lang in langHist) {
            getLangColor(lang);
            setLangCount(lang,langHist[lang]);
        }
        
        var l;
        for(var li = ls.length-1; li >=0; li-- ) {
            var l = ls[li];
            l.source = nodeIndex[l.project1];
            l.target = nodeIndex[l.project2];
        }
        
        nodes = ns;
        links = ls;
        
        $( "#totalNodesLabel" ).html( nodes.length )
        $( "#totalLinksLabel" ).html( links.length )
        updateStatus("data updated")
    }
    
    function updateGraph() {
        updateStatus("updating graph")
        force.stop();
        
        var langHist = {};        
        
        var pn;
        var possibleNodes = {};
        for (var pni = nodes.length-1; pni >= 0; pni--) {
            pn = nodes[pni];
            langHist[pn.lang] = 0;
            if ( selectedlangs[pn.lang] ) {
                possibleNodes[pn.id] = true;
            }
        }
        
        var l;
        var includedLinks = [];
        var linkedNodes = {};
        for (var li = links.length-1; li >= 0; li--) {
            l = links[li];
            if ( l.value >= minLinkValue &&
                 possibleNodes[l.source.id] &&
                 possibleNodes[l.target.id] ) {
                linkedNodes[l.source.id] = l.source;
                linkedNodes[l.target.id] = l.target;
                includedLinks.push(links[li]);
            }
        }

        var n;
        var includedNodes = [];
        for (var ni in linkedNodes) {
            n = linkedNodes[ni];
            includedNodes.push(n);
            langHist[n.lang] = (langHist[n.lang] || 0) + 1
        }

        for (var lang in langHist) {
            setLangViewCount(lang,langHist[lang]);
        }        
        
        $( "#visibleNodesLabel" ).html( includedNodes.length )
        $( "#visibleLinksLabel" ).html( includedLinks.length )
        
        force.nodes(includedNodes)
             .links(includedLinks);

        var link = vis.selectAll("line.link")
                      .data(includedLinks, function(l){
                          return  l.source.id+"-"+l.target.id; });
        
        link.enter()
            .append("svg:line")
            .attr("class", "link")
            .style("stroke-width", function(l) { 
                return Math.sqrt(l.value); })
            .style("stroke", "#CCC")
            .attr("x1", function(l) {
                return l.source.x; })
            .attr("y1", function(l) { 
                return l.source.y; })
            .attr("x2", function(l) { 
                return l.target.x; })
            .attr("y2", function(l) { 
                return l.target.y; })
            .on("mouseover", showLinkPopup)
            .on("mouseout", hidePopup);

        link.exit().remove();

        var node = vis.selectAll("circle.node")
                      .data(includedNodes, function(n){ 
                          return n.id; });

        node.enter()
            .append("svg:circle")
            .attr("class", "node")
            .attr("cx", function(n) { 
                return n.x; })
            .attr("cy", function(n) { 
                return n.y; })
            .attr("r", function(n) { 
                return n.value; })
            .style("fill", function(n) { 
                return getLangColor(n.lang); })
            .on("mouseover", showNodePopup)
            .on("mouseout", hidePopup)
            .call(force.drag);

        node.exit().remove();

        vis.on("mousemove", function() {

            fisheye.center(d3.mouse(this));

            node.each(function(n) { n.display = fisheye(n); })
                .attr("cx", function(n) {
                    return n.display.x; })
                .attr("cy", function(n) {
                    return n.display.y; })
                .attr("r", function(n) {
                    return n.display.z * n.value; });

            link.attr("x1", function(l) {
                return l.source.display.x; })
                .attr("y1", function(l) {
                    return l.source.display.y; })
                .attr("x2", function(l) {
                    return l.target.display.x; })
                .attr("y2", function(l) {
                    return l.target.display.y; });
        });
        
        updateStatus("graph updated")
        force.start();
    }

    function showLinkPopup(l) {
        var x = d3.event.x
        var y = d3.event.y
        showPopup("Link: "+l.source.name+" - "+l.target.name,
                [l.value+" common contributors"],
                [x,y]);
    }

    function showNodePopup(n) {
        showPopup("Project: "+n.name,
                  [n.desc || "",
                   "Language: "+(n.lang || UNKNOWN),
                   "Owner: "+(n.owner.name || n.owner.login || UNKNOWN),
                   n.value+" connected projects"],
                  [n.x,n.y]);
    }

    function showPopup(title,contents,pos) {
        $("#pop-up").fadeOut(100,function () {
            // Popup content
            $("#pop-up-title").html(title);
            $("#pop-up-content").html( "" );
            for (var i = 0; i < contents.length; i++) {
                $("#pop-up-content").append("<div>"+contents[i]+"</div>");
            }
            // Popup position
            var popLeft = (pos[0]*graphScale)+graphTrans[0]+20;
            var popTop = (pos[1]*graphScale)+graphTrans[1]+20;
            $("#pop-up").css({"left":popLeft,"top":popTop});
            $("#pop-up").fadeIn(100);
        });
    }
    
    function hidePopup(n) {
        $("#pop-up").fadeOut(50);
        d3.select(this).attr("fill","url(#ten1)");
    }

    ///////////////////////////////////////
    // Date range

    //var t = d3.scale.log().range([0, 10]).domain([10,20]);
    
    var histMargin = {top: 10, right: 10, bottom: 50, left: 50},
        histWidth = $( "#hist" ).width() - histMargin.left - histMargin.right,
        histHeight = $( "#hist" ).height() - histMargin.top - histMargin.bottom,
        histX = d3.time.scale().range([0, histWidth]),
        histY = d3.scale.log().range([histHeight,0]);
    
    var histXAxis = d3.svg.axis()
                          .scale(histX)
                          .ticks(d3.time.years,1)
                          .tickFormat(d3.time.format("%Y"))
                          .orient("bottom");

    var histYAxis = d3.svg.axis()
                          .scale(histY)
                          .ticks(1)
                          .orient("left");

    var histBrush = d3.svg.brush()
                          .x(histX)
                          .on("brush", onHistBrush)
                          .on("brushend", onHistBrushEnd);
    
    var histArea = d3.svg.line()
                         .x(function(d) {
                             return histX(d.date); })
                         .y(function(d) {
                             return histY(d.count); });

    var hist = d3.select( "#hist" )
                 .append("svg")
                 .attr("width", histWidth + histMargin.left + histMargin.right)
                 .attr("height", histHeight + histMargin.top + histMargin.bottom)
                 
    var histGraph = hist.append("g")
                        .attr("transform", "translate(" + histMargin.left + "," + histMargin.top + ")")
                        .attr( "class", "hist-graph" );
    
    var histContext = hist.append("g")
                          .attr("transform", "translate(" + histMargin.left + "," + histMargin.top + ")")
                          .attr( "class", "hist-context" );

    var histXAxisG = histContext.append("g")
                                .attr( "class", "hist-axis" )
                                .attr("transform", "translate(0," + histHeight + ")");

    var histYAxisG = histContext.append("g")
                                .attr( "class", "hist-axis" );

    histContext.append("g")
               .attr("class", "hist-brush" )
               .call(histBrush)
               .selectAll("rect")
               .attr("y", -6)
               .attr("height", histHeight + 7);        

    function onHistBrush() {
        if ( !histBrush.empty() ) {
            updateDates(histBrush.extent());
        }
    }    
    
    function onHistBrushEnd() {
        if ( histBrush.empty() ) {
            updateData([],[]);
        } else {
            requestGraphData();
        }
    }
    
    function updateHistData(data) {
        for (var i = data.length-1; i >= 0; i--) {
            data[i].date = new Date(data[i].date*1000);
        }
        histX.domain(d3.extent(data, function(d) {
            return d.date; }));
        histY.domain([1, d3.max(data, function(d) {
            return d.count; })]);

        histGraph.append("path")
           .datum(data)
           .attr("d", histArea);
        
        histXAxisG.call(histXAxis);
        histYAxisG.call(histYAxis);
        histXAxisG.selectAll("text")
                 .attr("transform"," translate(15,10) rotate(45)");
    }
    
    function updateDates(vals) {
        minTime = Math.round(vals[0].getTime() / 1000);
        maxTime = Math.round(vals[1].getTime() / 1000);
    }    
    
    ///////////////////////////////////////
    // Min link value

    $( "#min-link-value-slider" ).slider({
        orientation: "vertical",
        range: false,
        min: 1,
        max: 25,
        value: minLinkValue,
        slide: function( event, ui ) {
            updateMinLinkValue(ui.value);
        },
        stop: function( event, ui ) {
            updateMinLinkValue(ui.value);
            requestGraphData();
        }
    });
    
    function updateLinkValueLimit(max) {
        $( "#min-link-value-slider" ).slider( "option", "max", max);
        minLinkValue = Math.min(max,minLinkValue);

    }    
    
    function updateMinLinkValue(val) {
            minLinkValue = val;
        $('#min-link-value').text( minLinkValue );
    }

    updateMinLinkValue(minLinkValue);
    
    ///////////////////////////////////////
    // Query graph    

    function updateStatus(msg) {
        $("#statusLabel").text(msg);
    }

    var request = null;
    function requestGraphData() {
        if(request !== null) {
            updateStatus("try again later - request in progress");
        } else {
            updateStatus('loading...');
            request = $.getJSON('/links?from='+minTime+'&to='+maxTime+'&minWeight='+minLinkValue)
                       .success(function(json){
                            request = null;
                            if ( !$.isEmptyObject(json) ) {
                                updateData(json.projects,json.links);
                                updateGraph();
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

    updateStatus('getting date histogram');
    $.getJSON('/hist')
     .success(function(data){
          updateHistData(data);
          //minTime = maxTime = range.min;
          //updateDateLimits(range.min,range.max,range.step);
          //updateMinLinkValue(maximum range)
          updateStatus('ready');
      });

});
