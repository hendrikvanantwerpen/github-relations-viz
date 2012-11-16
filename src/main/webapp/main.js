$(document).ready(function(){

    var UNKNOWN = '[unknown]'

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
    // LANG
    
    var LANG = (new function(){
        var self = this;

        var LANG_DIV = "#languages";
        var color = d3.scale.category20();
        var colormap = {}
        var btnmap = {}
        var cntmap = {}
        var viewmap = {}
        var selected = {};
        
        self.getColor = function(lang) {
            var c = colormap[lang];
            if ( !c ) {
                c = color(lang);
                colormap[lang] = c;
                selected[lang] = true;
                var btn = $('<div/>',{
                    class: 'langButton',
                    style: 'background: '+c+';'
                }).button({
                    "label": lang || UNKNOWN
                }).appendTo( LANG_DIV )[0];
                btnmap[lang] = btn;
                $( btn ).click(function(e){
                    WAITING.busy();
                    if ( selected[lang] ) {
                        $( btn ).css( "background", "inherit" );
                        selected[lang] = false;
                    } else {
                        $( btn ).css( "background", c );
                        selected[lang] = true;
                    }
                    GRAPH.update();
                    WAITING.done();
                });
            }
            return c;
        };

        self.setTotals = function(totals) {
            mapO(cntmap,function(lang,count){
                return 0;
            });
            mergeO(cntmap,totals);
            forEachO(totals,function(lang,total){
                self.getColor(lang);
                cntmap[lang] = total;
                $( btnmap[lang] ).button( "option", "label", (lang || UNKNOWN) + " (0/" + total + ")" );
            });
        };

        self.setVisibles = function(visibles) {
            forEachO(cntmap,function(lang,total){
                self.getColor(lang);
                $( btnmap[lang] ).button( "option", "label", (lang || UNKNOWN) + " ("+(visibles[lang] || 0)+"/" + total + ")" );
            });
        };

        self.getSelected = function() {
            return selected;
        };

    });
    
    ///////////////////////////////////////
    // DATA
    
    var DATA = (new function(){
        var self = this;

        var minTime = maxTime = 0;
        var minLinkValue = 1;

        var nodes = {};
        var nodeCount = 0;
        var links = {};
        var linkCount = 0;

        var visibleNodes = [];
        var visibleLinks = [];
        
        var nodesByLang = {};

        self.setData = function(ns,ls) {
            STATUS.update("Updating data...")

            var newNodes = {};
            nodesByLang = {};
            nodeCount = 0;
            forEachA(ns,function(n) {
                nodeCount += 1;
                newNodes[n.id] = nodes[n.id] ? mergeO(nodes[n.id],n) : n;
                if ( nodesByLang[n.lang] ) {
                    nodesByLang[n.lang].push(n);
                } else {
                    nodesByLang[n.lang] = [n];
                }
            });
            
            var newLinks = {};
            linkCount = 0;
            forEachA(ls,function(l){
                linkCount += 1;
                l.source = newNodes[l.project1];
                l.target = newNodes[l.project2];
                newLinks[l.source.id+"-"+l.target.id] = l;
            });

            nodes = newNodes;
            links = newLinks;
            
            LANG.setTotals(mapO(nodesByLang,function(lang,lns){
                return lns.length;
            }));

            $( "#totalNodesLabel" ).text( nodeCount );
            $( "#totalLinksLabel" ).text( linkCount );
            $( "#visibleNodesLabel" ).text( "0" );
            $( "#visibleLinksLabel" ).text( "0" );
            
            STATUS.update("Data updated.");
        };

        self.setTimeRange = function(mint,maxt) {
            minTime = mint;
            maxTime = maxt;
        };

        self.setMinLinkValue = function(minlv) {
            minLinkValue = minlv;
        };        
        
        self.getNodes = function(){
            return visibleNodes;
        };
        
        self.getLinks = function(){
            return visibleLinks;
        };
        
        self.getMinLinkValue = function() {
            return minLinkValue;
        }
        
        self.getTimeRange = function() {
            return [minTime,maxTime];
        }

        self.filterData = function() {

            var selectedlangs = LANG.getSelected();

            var possibleNodes = {};
            forEachO(nodesByLang,function(lang,nodes){
                if ( selectedlangs[lang] ) {
                    forEachA(nodes,function(node){
                        possibleNodes[node.id] = node;
                    });
                }
            });

            visibleLinks = [];
            var linkedNodes = {};
            forEachO(links,function(id,link){
                if ( link.value >= minLinkValue &&
                     possibleNodes[link.source.id] &&
                     possibleNodes[link.target.id] ) {
                        linkedNodes[link.source.id] = link.source;
                        linkedNodes[link.target.id] = link.target;
                        visibleLinks.push(link);
                    }
            });

            visibleNodes = [];
            var langHist = {};
            forEachO(linkedNodes,function(id,node){
                langHist[node.lang] = (langHist[node.lang] || 0) + 1;
                visibleNodes.push(node);
            });
            LANG.setVisibles(langHist);

            $( "#visibleNodesLabel" ).text( visibleNodes.length );
            $( "#visibleLinksLabel" ).text( visibleLinks.length );

        };
        
    });
    
    ///////////////////////////////////////
    // D3 Graph
    
    var GRAPH = (new function(){
        var self = this;

        var graphWidth = $( "#graph" ).width(),
            graphHeight = $( "#graph" ).height(),
            graphTrans = [0,0],
            graphScale = 1;

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

        var background = vis.append('svg:g')
                            .append('svg:rect')
                            .attr('width', graphWidth)
                            .attr('height', graphHeight)
                            .attr('fill', 'white');
        var linkVis = vis.append('svg:g');
        var nodeVis = vis.append('svg:g');

        function onzoom() {
            graphScale = d3.event.scale
            graphTrans = d3.event.translate
            nodeVis.attr("transform","translate("+graphTrans+")"+" scale("+graphScale+")");
            linkVis.attr("transform","translate("+graphTrans+")"+" scale("+graphScale+")");
        }

        var force = d3.layout.force()
                      .size([graphWidth, graphHeight])
                      .charge(-500)
                      .linkDistance(function(link){
                          var d = link.source.value+link.target.value;
                          return 3*d; })
                      .linkStrength( 0.1 );

        self.update = function() {
            STATUS.update("Updating graph...")
            force.stop();

            DATA.filterData();
            var includedNodes = DATA.getNodes();
            var includedLinks = DATA.getLinks();

            force.nodes(includedNodes)
                 .links(includedLinks);

            var link = linkVis.selectAll("line.link")
                              .data(includedLinks, function(l){
                                   return l.source.id+"-"+l.target.id; });

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

            var node = nodeVis.selectAll("circle.node")
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
                    return LANG.getColor(n.lang); })
                .on("mouseover", showNodePopup)
                .on("mouseout", hidePopup)
                .call(force.drag);

            node.exit().remove();

            force.on("tick", function() {
                node.attr("cx", function(n) {
                         return n.x; })
                    .attr("cy", function(n) {
                         return n.y; })
                    .attr("r", function(n) {
                         return n.value; });
                link.attr("x1", function(l) {
                         return l.source.x; })
                    .attr("y1", function(l) {
                         return l.source.y; })
                    .attr("x2", function(l) {
                         return l.target.x; })
                    .attr("y2", function(l) {
                         return l.target.y; });
            });
            /*vis.on("mouseover", function() {
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
            });*/

            STATUS.update("Graph updated.")
            force.start();
        };

        function showLinkPopup(l) {
            var x = d3.event.x
            var y = d3.event.y
            showPopup("Link: "+l.source.name+" - "+l.target.name,
                    [l.value+" common contributors"],
                    [x,y]);
        }
    
        function showNodePopup(n) {
        	var x = (n.x*graphScale)+graphTrans[0];
        	var y = (n.y*graphScale)+graphTrans[1];
            showPopup("Project: "+n.name,
                      [n.desc || "",
                       "Language: "+(n.lang || UNKNOWN),
                       "Owner: "+(n.owner.name || n.owner.login || UNKNOWN),
                       n.value+" connected projects"],
                       [x,y]);
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
                var popLeft = pos[0]+20;
                var popTop  = pos[1]+20;
                $("#pop-up").css({"left":popLeft,"top":popTop});
                $("#pop-up").fadeIn(100);
            });
        }
        
        function hidePopup(n) {
            $("#pop-up").fadeOut(50);
            d3.select(this).attr("fill","url(#ten1)");
        }
    
    });
    
    ///////////////////////////////////////
    // Date range

    var HIST = (new function(){
        var self = this;
        
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
                var e = histBrush.extent();
                DATA.setTimeRange(Math.round(e[0].getTime() / 1000),
                                  Math.round(e[1].getTime() / 1000));
            }
        }    
        
        function onHistBrushEnd() {
            if ( histBrush.empty() ) {
                DATA.setData([],[]);
                GRAPH.update();
            } else {
                REST.requestGraphData();
            }
        }
        
        self.setData = function setData(data) {
            STATUS.update("Setting activity histogram.");
            
            forEachA(data,function(d){
                d.date = new Date(d.date*1000);
            });
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
            STATUS.update("Histogram rendered. Ready.");
        };

    });
    
    ///////////////////////////////////////
    // Min link value

    var LINKSLIDER = (new function(){
        var self = this;
        
        $( "#min-link-value-slider" ).slider({
            orientation: "vertical",
            range: false,
            min: 2,
            max: 25,
            value: 1,
            slide: function( event, ui ) {
                updateMinLinkValue(ui.value);
            },
            stop: function( event, ui ) {
                updateMinLinkValue(ui.value);
                REST.requestGraphData();
            }
        });
        
        self.setMaxValue = function(max) {
            $( "#min-link-value-slider" ).slider( "option", "max", max);
            DATA.setMinLinkValue(Math.min(max,DATA.getMinLinkValue()));
        };
        
        function updateMinLinkValue(val) {
            DATA.setMinLinkValue(val);
            $('#min-link-value').text( val );
        }
        
        updateMinLinkValue(2);
    });
    
    
    ///////////////////////////////////////
    // Query graph

    var STATUS = (new function(){
        var self = this;
        self.update = function(msg) {
            $("#statusLabel").text(msg);
        };
    });

    var REST = (new function(){
        var self = this;
        
        var request = null;
        self.requestGraphData = function() {
            if(request !== null) {
                STATUS.update("try again later - request in progress");
            } else {
                WAITING.busy();
                STATUS.update('loading...');
                var timeRange = DATA.getTimeRange();
                var minLinkValue = DATA.getMinLinkValue();
                request =
                    $.getJSON('/links?from='+timeRange[0]+'&to='+timeRange[1]+'&minWeight='+minLinkValue)
                     .success(function(json){
                          if ( json.error ) {
                              STATUS.update(json.error+" Graph not updated.");
                          } else {
                              DATA.setData(json.projects,json.links);
                              GRAPH.update();
                          }
                          request = null;
                          WAITING.done();
                      })
                     .error(function(){
                          STATUS.update('Request error. Graph not updated.');
                          request = null;
                          WAITING.done();
                      });
            }
        };
        
        self.requestHistData = function() {
            STATUS.update('Getting histogram...');
            WAITING.busy();
            $.getJSON('/hist')
             .success(function(data){
                  HIST.setData(data);
                  WAITING.done();
              })
             .error(function(error){
                 STATUS.update("Error when requesting histogram data.");
                 WAITING.done();
             });            
        };
        
    });
    
    ///////////////////////////////////////
    // Waiting indicator    
    
    var WAITING = (new function(){
        var self = this;

        self.busy = function() {
            var offset = $( "#controls" ).offset();
            var width = $( "#controls" ).width();
            var height = $( "#controls" ).height();
            $( "#waiting" ).css({
                "top": offset.top+"px",
                "left": offset.left+"px",
                "width": width+"px",
                "height": height+"px"
            });
            $( "#waiting" ).fadeIn(100);
        };
        
        self.done = function() {
            $("#waiting").fadeOut(100);
        };
    });
    
    REST.requestHistData();

    ///////////////////////////////////////
    // Utils
    
    function forEachA(array,f) {
        var n = array.length;
        for ( var i = 0; i < n; i++ ) {
            f(array[i],i);
        }
    }

    function mapA(array,f) {
        var newArray = [];
        var n = array.length;
        for ( var i = 0; i < n; i++ ) {
            newArray.push(f(array[i],i));
        }
        return array;
    }

    function forEachO(obj,f) {
        for ( var prop in obj ) {
            if ( obj.hasOwnProperty(prop) ) {
                f(prop,obj[prop]);
            }
        }
    }
    
    function mapO(obj,f) {
        var newObj = {};
        for ( var prop in obj ) {
            if ( obj.hasOwnProperty(prop) ) {
                newObj[prop] = f(prop,obj[prop]);
            }
        }
        return newObj;
    }
    
    function mergeO(obj,newObj) {
        for ( var prop in newObj ) {
            if ( newObj.hasOwnProperty(prop) ) {
                obj[prop] = newObj[prop];
            }
        }
        return obj;
    }
    
});
