define(["jquery","rx","d3","lang"],function($,Rx,d3,LANG){
	
	var UNKNOWN = "[unknown]";
	
    var nodesAndLinks = new Rx.Subject();
    var status = new Rx.Subject();
    
    var graphWidth = $( "#graph" ).width(),
        graphHeight = $( "#graph" ).height(),
        graphTrans = [0,0],
        graphScale = 1;

    var zoomer = d3.behavior.zoom();
    
    var vis = d3.select("#graph")
                .append("svg:svg")
                .attr("width", graphWidth)
                .attr("height", graphHeight)
                .attr("pointer-events", "all")
                .append('svg:g')
                .call(zoomer.on("zoom", onzoom ))
                .on("dblclick.zoom", null);

    var background = vis.append('svg:g')
                        .append('svg:rect')
                        .attr('width', graphWidth)
                        .attr('height', graphHeight)
                        .attr('fill', 'white');
    var linkVis = vis.append('svg:g');
    var nodeVis = vis.append('svg:g');

    function onzoom() {
        graphScale = zoomer.scale();
        graphTrans = zoomer.translate();
        nodeVis.attr("transform","translate("+graphTrans+")"+" scale("+graphScale+")");
        linkVis.attr("transform","translate("+graphTrans+")"+" scale("+graphScale+")");
    }

    var force = d3.layout.force()
                  .size([graphWidth, graphHeight])
                  .charge(-500)
                  .linkDistance(function(link){
                      var d = link.source.value+link.target.value;
                      return 3*d; })
                  .linkStrength( 0.1 )
                  .gravity( 0.1 );

    nodesAndLinks.subscribe(function(d) {
        status.onNext("Updating graph...")
        force.stop();

        var includedNodes = d.nodes;
        var includedLinks = d.links;

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
            .attr("class", function(n){ return "node"+(n.fixed ? " fixed" : ""); })
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
            .on("dblclick", toggleNodeFixation)
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

        status.onNext("Graph updated.")
        force.start();
    });

    function toggleNodeFixation(n) {
        n.fixed = !n.fixed;
        d3.select(this)
          .classed("fixed", n.fixed);
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
        var x = (n.x*graphScale)+graphTrans[0];
        var y = (n.y*graphScale)+graphTrans[1];
        showPopup("Project: "+n.name,
                  [n.desc || "",
                   "Language: "+(n.lang || UNKNOWN),
                   "Owner: "+(n.owner.name || n.owner.login || UNKNOWN),
                   "Url: <a target=\"_blank\" href=\""+n.url+"\">"+n.url+"</a>",
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
    
    var eff = null;
    $("#pop-up").mouseover(function(e){
        if ( eff ) {
            eff.stop( true );
            $("#pop-up").fadeIn(50);
            eff = null;
        }
    });
    $("#pop-up").mouseleave(function(e){
        $("#pop-up").fadeOut(50);
    })
    
    function hidePopup() {
        eff = $("#pop-up").delay(100).fadeOut(50);
        d3.select(this).attr("fill","url(#ten1)");
    }

    $("#pop-up").dblclick( function(e) {
        hidePopup();
        e.preventDefault();
    });
    
    $( "#resetZoom" ).click( function(e) {
        var xe = d3.extent(force.nodes(),function(n){ return n.x; });
        var ye = d3.extent(force.nodes(),function(n){ return n.y; });
        var s = Math.min( graphWidth / (xe[1]-xe[0]) , graphHeight / (ye[1]-ye[0]) );
        zoomer.translate( [0,0] );
        zoomer.scale( s );
        e.preventDefault();
    });
    
    return {
    	nodesAndLinks: nodesAndLinks,
    	status: status.asObservable()
    };
	
});