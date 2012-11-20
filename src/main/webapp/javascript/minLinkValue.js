define(["jquery","rx","jquery-ui","rx.binding"],function($,Rx){
	
    var value = new Rx.BehaviorSubject();
    
    $( "#min-link-value-slider" ).slider({
        orientation: "vertical",
        range: false,
        min: 1,
        max: 25,
        value: 2,
        slide: function( event, ui ) {
            $('#min-link-value').text( ui.value );
        },
        stop: function( event, ui ) {
            value.onNext(ui.value);
        }
    });
    
    value.onNext(2);
    
    return {
    	value: value.asObservable()
    };
    
});