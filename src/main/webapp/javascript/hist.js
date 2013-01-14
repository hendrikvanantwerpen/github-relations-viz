define(["jquery","rx","waiter","util","rx.binding"],function($,Rx,Waiter,util){
	
	var waiter = new Waiter( "#hist" );
	
    var selection = new Rx.BehaviorSubject();
    var status = new Rx.Subject();
    var langsFilter = new Rx.Subject();
    
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
                          .on("brushend", onHistBrushEnd);

    var histArea = d3.svg.line()
                     .x(function(d) {
                            return histX(d.date); })
                     .y(function(d) {
                            return histY(d.count); });

    var hist = d3.select("#hist")
                 .append("svg")
                 .attr("width", histWidth + histMargin.left + histMargin.right)
                 .attr("height", histHeight + histMargin.top + histMargin.bottom)

    var histGraphAll = hist.append("g")
                        .attr("transform", "translate(" + histMargin.left + "," + histMargin.top + ")")
                        .attr( "class", "hist-graph-all" );
                 
    var histGraphCurr = hist.append("g")
                            .attr("transform", "translate(" + histMargin.left + "," + histMargin.top + ")")
                            .attr( "class", "hist-graph-current" );
    
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

    function onHistBrushEnd() {
        if ( histBrush.empty() ) {
            selection.onNext(null);
        } else {
            var e = histBrush.extent();
            selection.onNext([
                Math.round(e[0].getTime() / 1000),
                Math.round(e[1].getTime() / 1000)]);
        }
    }
    
    function convertData(data) {
    	data = util.arrays.filter(data,function(d){ return d.count >= 1; });
        util.arrays.forEach(data,function(d){
            d.date = new Date(d.date*1000);
        });
        var n = data.length;
        if ( n > 0 ) {
            data.push({
            	date: data[n-1].date,
            	count: 1
            });
        	data.unshift({
        		date: data[0].date,
        		count: 1
        	});
        }
    	return data;
    }
    
    function updateAll(data) {
        status.onNext("Setting full activity histogram.");
        
        histX.domain(d3.extent(data, function(d) {
            return d.date; }));
        histY.domain([1, d3.max(data, function(d) {
            return d.count; })]);

        histGraphAll.append("path")
           .datum(data)
           .attr("d", histArea);
        
        histXAxisG.call(histXAxis);
        histYAxisG.call(histYAxis);
        histXAxisG.selectAll("text")
                 .attr("transform"," translate(15,10) rotate(45)");

        status.onNext("Full histogram rendered. Ready.");
    }
    
    function updateCurr(data) {
        status.onNext("Setting current activity histogram.");
        
        histGraphCurr.select("*").remove();
        histGraphCurr.append("path")
           .datum(data)
           .attr("d", histArea);

        status.onNext("Current histogram rendered. Ready.");
    };

    langsFilter.subscribe(function(langsFilter){
        waiter.busy();
        $.getJSON('/hist?langs_strict='+langsFilter.strict+
        		  '&include_langs='+encodeURIComponent(JSON.stringify(langsFilter.include))+
        		  '&exclude_langs='+encodeURIComponent(JSON.stringify(langsFilter.exclude)))
         .success(function(data){
              updateCurr(convertData(data));
              waiter.done();
          })
         .error(function(error){
             status.onNext("Error when requesting histogram data.");
             waiter.done();
         });        	
    });
    
    waiter.busy();
    $.getJSON('/hist')
     .success(function(data){
          updateAll(convertData(data));
          waiter.done();
      })
     .error(function(error){
         status.onNext("Error when requesting full histogram data.");
         waiter.done();
     });        	
    
    selection.onNext(null);
    
    return {
        selection: selection.asObservable(),
        status: status.asObservable(),
        langsFilter: langsFilter
    };
	
});