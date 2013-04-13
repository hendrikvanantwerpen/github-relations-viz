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
define(["jquery","rx","waiter","util","rx.binding"],function($,Rx,Waiter,util){
	
	var waiter = new Waiter( "#controls" );
	
    var projectMap = {};
    var userMap = {};

    var nodesAndLinks = new Rx.BehaviorSubject();
    var langCount = new Rx.BehaviorSubject();
    var timeRange = new Rx.Subject();
    var minLinkValue = new Rx.Subject();
    var langsFilter = new Rx.Subject();
    var status = new Rx.Subject();

    var request = null;
    timeRange.combineLatest(langsFilter,minLinkValue,function(timeRange,langsFilter,minLinkValue){
        return {
            timeRange: timeRange,
            langsFilter: langsFilter,
            minLinkValue: minLinkValue
        }
    }).subscribe(function(d) {
        if ( !d.timeRange ) {
            update([],[]);
            return;
        }
        if(request !== null) {
            status.onNext("try again later - request in progress");
        } else {
            waiter.busy();
            status.onNext('loading...');
            request =
                $.getJSON('/links?from='+d.timeRange[0]+
                          '&to='+d.timeRange[1]+
                          '&langs_strict='+d.langsFilter.strict+
                          '&include_langs='+encodeURIComponent(JSON.stringify(d.langsFilter.include))+
                          '&exclude_langs='+encodeURIComponent(JSON.stringify(d.langsFilter.exclude))+
                          '&min_link_weight='+d.minLinkValue+
                          '&limit=5000')
                 .success(function(json){
                      if ( json.error ) {
                    	  status.onNext(json.error+" Graph not updated.");
                      } else {
                          update(json.projects,json.links);
                      }
                      request = null;
                      waiter.done();
                  })
                 .error(function(){
                	 status.onNext('Request error. Graph not updated.');
                      request = null;
                      waiter.done();
                  });
        }
    });
    
    function update(ns,ls) {
    	status.onNext("Updating data...")

        var lc = {};
        
        // WARNING: We keep every node we've even seen in memory
        // so the graph is more stable. This could result in problems!
        util.arrays.forEach(ns,function(n,i) {
            var userId = n.owner.id;
            var projectId = n.id;
            var userURL = "https://github.com/"+n.owner.login;
            var projectURL = userURL+"/"+n.name;
            
            lc[n.lang] = (lc[n.lang] || 0) + 1;
            userMap[userId] = util.objects.merge(userMap[userId] || { url: userURL }, n.owner);
            projectMap[projectId] = util.objects.merge(projectMap[projectId] || { url: projectURL }, n)

            ns[i] = projectMap[projectId];
            n.owner = userMap[userId];
        });
        
        util.arrays.forEach(ls,function(l){
            l.source = projectMap[l.project1];
            l.target = projectMap[l.project2];
        });
        
        nodesAndLinks.onNext({
            nodes:ns,
            links:ls
        });
        langCount.onNext(lc);
        
        status.onNext("Data updated.");
    };

    return {
        nodesAndLinks: nodesAndLinks.asObservable(),
        langCount: langCount.asObservable(),
        status: status.asObservable(),
        
        timeRange: timeRange,
        minLinkValue: minLinkValue,
        langsFilter: langsFilter
    };

});
