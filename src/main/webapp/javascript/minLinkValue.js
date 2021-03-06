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
