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
