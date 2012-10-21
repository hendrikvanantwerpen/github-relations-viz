$(document).ready(function(){

  $.ajaxSetup({
      cache: false
  });

  var labelType, useGradients, nativeTextSupport, animate;

  var minTime = maxTime = 0
  var degree = 1

  var ua = navigator.userAgent,
      iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
      typeOfCanvas = typeof HTMLCanvasElement,
      nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
      textSupport = nativeCanvasSupport 
        && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
  //I'm setting this based on the fact that ExCanvas provides text support for IE
  //and that as of today iPhone/iPad current text support is lame
  labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
  nativeTextSupport = labelType == 'Native';
  useGradients = nativeCanvasSupport;
  animate = !(iStuff || !nativeCanvasSupport);

  // init ForceDirected
  var fd = new $jit.ForceDirected({
    //id of the visualization container
    injectInto: 'graphanchor',
    //Enable zooming and panning
    //by scrolling and DnD
    Navigation: {
      enable: true,
      //Enable panning events only if we're dragging the empty
      //canvas (and not a node).
      panning: 'avoid nodes',
      zooming: 10 //zoom speed. higher is more sensible
    },
    // Change node and edge styles such as
    // color and width.
    // These properties are also set per node
    // with dollar prefixed data-properties in the
    // JSON structure.
    Node: {
      overridable: true
    },
    Edge: {
      overridable: true,
      color: '#23A4FF',
      lineWidth: 0.4
    },
    //Native canvas text styling
    Label: {
      type: labelType, //Native or HTML
      size: 4,
      color: 'black',
      style: 'bold'
    },
    //Add Tips
    Tips: {
      enable: true,
      onShow: function(tip, node) {
        //count connections
        var count = 0;
        node.eachAdjacency(function() { count++; });
        //display node info in tooltip
        tip.innerHTML = "<div class=\"tip-title\">" + node.name + "</div>"
          + "<div class=\"tip-text\"><b>connections:</b> " + count + "</div>";
      }
    },
    // Add node events
    Events: {
      enable: true,
      type: 'Native',
      //Change cursor style when hovering a node
      onMouseEnter: function() {
        fd.canvas.getElement().style.cursor = 'move';
      },
      onMouseLeave: function() {
        fd.canvas.getElement().style.cursor = '';
      },
      //Add also a click handler to nodes
      onClick: function(node) {
        if(!node) return;
        // Build the right column relations list.
        // This is done by traversing the clicked node connections.
        var html = "<h4>" + node.name + "</h4><b> connections:</b><ul><li>",
            list = [];
        node.eachAdjacency(function(adj){
          list.push(adj.nodeTo.name);
        });
        //append connections information
        $jit.id('inner-details').innerHTML = html + list.join("</li><li>") + "</li></ul>";
      }
    },
    //Number of iterations for the FD algorithm
    iterations: 10,
    //Edge length
    levelDistance: 130,
    // Add text to the labels. This method is only triggered
    // on label creation and only for DOM labels (not native canvas ones).
    onCreateLabel: function(domElement, node){
      domElement.innerHTML = node.name;
      var style = domElement.style;
      style.fontSize = "0.8em";
      style.color = "#ddd";
    },
    // Change node styles when DOM labels are placed
    // or moved.
    onPlaceLabel: function(domElement, node){
      var style = domElement.style;
      var left = parseInt(style.left);
      var top = parseInt(style.top);
      var w = domElement.offsetWidth;
      style.left = (left - w / 2) + 'px';
      style.top = (top + 10) + 'px';
      style.display = '';
    }
  });

  function createUI(minDate,maxDate) {
    $( "#date-range-slider" ).slider({
      range: true,
      min: minDate,
      max: maxDate,
      values: [ minTime, maxTime ],
      slide: function( event, ui ) {
          minTime = ui.values[ 0 ];
          maxTime = ui.values[ 1 ];
          updateUI();
      }
    });
    
    $( "#degree-range-slider" ).slider({
      range: false,
      min: 1,
      max: 20,
      values: [ degree ],
      slide: function( event, ui ) {
          degree = ui.values[0];
          updateUI();
      }
    });
    
    $( "#refresh" ).button()
    .click(function(e){
      refreshGraph();
      e.preventDefault();
    });
  }

  function fmtEpoch(e) {
      return new Date(1000*e).toString();
  }
  
  function updateUI() {
      $( "#date-range" ).text( fmtEpoch(minTime)+" - "+fmtEpoch(maxTime));
      $('#degree').text( degree );
  }

  var request = null;
  function refreshGraph() {
      if(request !== null) {
          request.abort();
          request = null;
          refreshGraph();
      } else {
        $("#statusLabel").text('loading...');
        request = $.getJSON('/jitdata?from='+minTime+'&to='+maxTime+'&degree='+degree)
        .success(function(json){
          if ( json.length > 0 ) {
            $("#statusLabel").text('loading json into graph');
            // load JSON data.
            fd.loadJSON(json);
            // compute positions incrementally and animate.
            fd.computeIncremental({
              iter: 200,
              property: 'end',
              onStep: function(perc){
                $("#statusLabel").text(perc+'% loaded...');
              },
              onComplete: function(){
                $("#statusLabel").text('animating graph');
                fd.animate({
                  modes: ['linear'],
                  transition: $jit.Trans.Elastic.easeOut,
                  duration: 2500
                });
                request = null;
                $("#statusLabel").text('graph rendered - ready');
              }
            });
          } else {
            request = null;
            $("#statusLabel").text('empty graph - ready');
          }
        })
        .error(function(){
         $("#statusLabel").text('error');
         request = null;
       });
     }
  }

  $("#statusLabel").text('getting date range');
  $.getJSON('/range')
  .success(function(range){
    minTime = maxTime = range.min
    createUI(range.min,range.max);
    $("#statusLabel").text('ready');
  });

});
