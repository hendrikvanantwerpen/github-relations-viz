/* Copyright 2012-2013 The Github-Relations-Viz Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
