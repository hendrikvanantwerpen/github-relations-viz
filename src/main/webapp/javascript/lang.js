define(["jquery","rx","waiter","util","rx.binding"],function($,Rx,Waiter,util){

    var UNKNOWN = '[unknown]';

    var LANG_DIV = "#languages";
	var waiter = new Waiter( LANG_DIV );
	
    var langsFilter = new Rx.BehaviorSubject();
    var timeRange = new Rx.Subject();
    var viewCount = new Rx.Subject();
    var status = new Rx.Subject();
    
    var color = d3.scale.category20();
    var colormap = {};
    var btnmap = {};
    var selected = {};
    var strict = true;
    
    $( "#langRelax" ).change(function(e){
        strict = e.target.checked;
    });
    
    function getColor(lang) {
        var c = colormap[lang];
        if ( !c ) {
            c = color(lang);
            colormap[lang] = c;
            selected[lang] = !strict;
            var btn = $('<div/>',{
                class: 'langButton',
                style: 'background: '+(strict ? "inherit" : c)+';'
            }).button({
                "label": (lang || UNKNOWN) + " (<span class=\"viewCount\">-</span>/<span class=\"totalCount\">-</span>)"
            }).appendTo( LANG_DIV )[0];
            btnmap[lang] = btn;
            $( btn ).click(function(e){
                selected[lang] = !selected[lang];
                $( btn ).css( "background", selected[lang] ? c : "inherit" );
                publish();
            });
        }
        return c;
    };

    function publish() {
    	var filter = {
    	    include: [],
    	    exclude: [],
    	    strict: strict
    	};
        util.objects.forEach(selected,function(lang,selected){
            (selected ? filter.include : filter.exclude).push(lang);
        });
        langsFilter.onNext(filter);
    }
    
    function update(langCount){
        var counts = util.objects.map(selected,function(){ return 0; });
        counts = util.objects.merge(counts,langCount);
        util.objects.forEach(counts,function(lang,total){
            getColor(lang);
            $( '.totalCount', btnmap[lang] ).html( total );
        });
    };

    timeRange.subscribe(function(timeRange){
        var req = "/langs";
        if ( timeRange ) {
            req = req+'?from='+timeRange[0]+
                      '&to='+timeRange[1];
        }
        waiter.busy();
        $.getJSON(req)
        .success(function(data){
             update(data);
             waiter.done();
         })
        .error(function(error){
        	status.onNext("Error when requesting histogram data.");
            waiter.done();
        });            
    });
    
    viewCount.subscribe(function(viewCount){
        var counts = util.objects.map(selected,function(){ return 0; });
        counts = util.objects.merge(counts,viewCount);
        util.objects.forEach(counts,function(lang,count){
            getColor(lang);
            $( '.viewCount', btnmap[lang] ).html( count );
        });
    });
    
    langsFilter.onNext({
    	include: [],
    	exclude: [],
    	strict: strict
    });
    
    return {
        langsFilter: langsFilter.asObservable(),
        status: status.asObservable(),
        timeRange: timeRange,
        viewCount: viewCount,
        getColor: getColor,
    };
	
});