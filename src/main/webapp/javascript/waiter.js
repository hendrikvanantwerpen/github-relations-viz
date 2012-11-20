define(["jquery"],function($){
	
	return function(element){
		var self = this;
		var w = $('<div class="waiter"></div>');
		$('body').append(w);
	    self.busy = function() {
	        var offset = $( element ).offset();
	        var width = $( element ).width();
	        var height = $( element ).height();
	        $( w ).css({
	            "top": offset.top+"px",
	            "left": offset.left+"px",
	            "width": width+"px",
	            "height": height+"px"
	        });
	        $( w ).fadeIn(100);
	    };
	    
	    self.done = function() {
	        $( w ).fadeOut(100);
	    };
	};
	

});