require(["jquery", "d3", "rx", "data", "lang", "hist", "graph", "minLinkValue", "rx.aggregates", "rx.binding", "rx.joinpatterns", "jquery-ui", "domReady"],
		function($,d3,Rx,DATA,LANG,HIST,GRAPH,MINLINKVALUE) {
	
    $.ajaxSetup({
        cache: false
    });

    HIST.selection.subscribe(DATA.timeRange);
    HIST.selection.subscribe(LANG.timeRange);
    MINLINKVALUE.value.subscribe(DATA.minLinkValue);
    LANG.langsFilter.subscribe(DATA.langsFilter);
    LANG.langsFilter.subscribe(HIST.langsFilter);
    
    LANG.status.merge(DATA.status,HIST.status,GRAPH.status).subscribe(function(s){
        $("#statusLabel").text(s);
    });

    DATA.nodesAndLinks.subscribe(function(d){
        $( "#totalNodesLabel" ).text( d.nodes.length ); 
    });
    DATA.nodesAndLinks.subscribe(function(d){
        $( "#totalLinksLabel" ).text( d.links.length );
    });
    DATA.nodesAndLinks.subscribe(GRAPH.nodesAndLinks);
    DATA.langCount.subscribe(LANG.viewCount);
    
});