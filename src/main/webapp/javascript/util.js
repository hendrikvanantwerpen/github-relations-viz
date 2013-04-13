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
define([],function(){
    return {
        arrays: {
            forEach: function(array,f) {
                var n = array.length;
                for ( var i = 0; i < n; i++ ) {
                    f(array[i],i);
                }
            },
            map: function(array,f) {
                var newArray = [];
                var n = array.length;
                for ( var i = 0; i < n; i++ ) {
                    newArray.push(f(array[i],i));
                }
                return newArray;
            },
            filter: function(array, f) {
                var newArray = [];
                var n = array.length;
                for ( var i = 0; i < n; i++ ) {
                    if (f(array[i],i) === true) {
                        newArray.push(array[i]);
                    }
                }
                return newArray;
            }
        },
        objects: {
            forEach: function(obj,f) {
                for ( var prop in obj ) {
                    if ( obj.hasOwnProperty(prop) ) {
                        f(prop,obj[prop]);
                    }
                }
            },
            map: function(obj,f) {
                var newObj = {};
                for ( var prop in obj ) {
                    if ( obj.hasOwnProperty(prop) ) {
                        newObj[prop] = f(prop,obj[prop]);
                    }
                }
                return newObj;
            },
            merge: function(obj,newObj) {
                for ( var prop in newObj ) {
                    if ( newObj.hasOwnProperty(prop) ) {
                        obj[prop] = newObj[prop];
                    }
                }
                return obj;
            }
        }
    };
});
