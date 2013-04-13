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
