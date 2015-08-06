/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var sunburstDirectiveModule = angular.module('sunburstDirectiveModule', ['sunburstControllerModule']);

sunburstDirectiveModule.directive('sunburstChart', function($location){
    function sunburstChart(data, element, scope){
        //sunburstSaver.resizeTemp = data;
        //scope.sunburstSaver.resizeTemp = sunburstSaver.resizeTemp;
        var ele = element[0];
        var width = (window.innerWidth), height = (window.innerHeight*.8);
        var margin = {top: height/2, right: width/2, bottom: height/2, left: width/2},
            radius = Math.min(margin.top, margin.right, margin.bottom, margin.left) - Math.min(height,width)*.15;

        function filter_min_arc_size_text(d, i) {return (d.dx*d.depth*radius/3)>5}; 

        var hue = d3.scale.category10();

        var luminance = d3.scale.sqrt()
            .domain([0, 1e6])
            .clamp(true)
            .range([90, 20]);

        //if no data is available show a message
        if (data === 0){
            console.log("no data")
            d3.select(ele).select("svg").remove();
            d3.select(ele).select("#tooltip").remove();
          var svg = d3.select(ele).append("svg")
                .attr("width", margin.left + margin.right)
                .attr("height", margin.top + margin.bottom)
              .append("g")
                .attr("transform", "translate(" + (width-60)/2 + "," + margin.top + ")");
              
          svg.append("text")
                .text("No Data Available")
          return;
        }
        //remove SVG before appending. To be replaced by transition.
        d3.select(ele).select("svg").remove();
        d3.select(ele).select("#tooltip").remove();
        var svg = d3.select(ele).append("svg")
            .attr("width", margin.left + margin.right)
            .attr("height", margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + (width-60)/2 + "," + margin.top + ")");
    
    
    

        var partition = d3.layout.partition()
            .sort(function(a, b) { return d3.ascending(a.name, b.name); })
            .size([2 * Math.PI, radius]);

        var arc = d3.svg.arc()
            .startAngle(function(d) { return d.x; })
            .endAngle(function(d) { return d.x + d.dx - .01 / (d.depth + .5); })
            .innerRadius(function(d) { return radius / 3 * d.depth; })
            .outerRadius(function(d) { return radius / 3 * (d.depth + 1) - 1; });

        //Tooltip description
        var tooltip = d3.select(ele)
            .append("div")
            .attr("id", "tooltip")
            .style("position", "absolute")
            .style("z-index", "5")
            .style("opacity", 0);

        function format_number(x) {
          return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        };
        function format_description(d) {
          var description = d.description;
              return  '<b>' + d.name + '</b></br>' + format_number(d.value) + '';
        };
        function computeTextRotation(d) {
            var angle=(d.x +d.dx/2)*180/Math.PI - 90	
            return angle;
        };
        function mouseOverArc(d) {
            d3.select(this).attr("stroke","black")
            tooltip.html(format_description(d));
            return tooltip.transition()
            .duration(50)
            .style("opacity", 0.9);
        };
        function mouseOutArc(){
            d3.select(this).attr("stroke","")
            return tooltip.style("opacity", 0);
        };
        function mouseMoveArc (d) {
            return tooltip
            .style("top", (d3.event.pageY-275)+"px")
            .style("left", (d3.event.pageX+10)+"px");
        };//location of tooltip
        function createSunburst(root, scope){
            partition
              .value(function(d) { return d.size; })
              .nodes(root)
              .forEach(function(d) {
                d._children = d.children;
                d.sum = d.value;
                d.key = key(d);
                d.fill = fill(d);
              });

          // Now redefine the value function to use the previously-computed sum.
            partition
                .children(function(d, depth) { 
                    return depth < 3 ? d._children : null; 
                })
                .value(function(d) { return d.sum; });



            var center = svg.append("circle")
                .attr("r", radius / 5)
                .on("click", zoomOut);

            center.append("title")
              .text("zoom out");
          
            var partitioned_data= partition.nodes(root).slice(1);
         
            
          
            var path = svg.selectAll("path")
                .data(partitioned_data)
              .enter().append("path")
                .attr("d", arc)
                .style("fill", function(d) { return d.fill; })
                .each(function(d) { this._current = updateArc(d); })
                .on("click", function(d){zoomIn(d, scope);})
                .on("mouseover", mouseOverArc)
                .on("mousemove", mouseMoveArc)
                .on("mouseout", mouseOutArc);


            var texts = svg.selectAll("text")
              .data(partitioned_data)
            .enter().append("text")
                  .attr("transform", function(d) { return computeTextRotation(d)<90?"rotate(" + computeTextRotation(d) + ")":"rotate(" + computeTextRotation(d) + ")rotate(-180)"; })
                  .attr("text-anchor", function(d){return computeTextRotation(d)<90? "start":"end";})
                  .attr("x", function(d) { return computeTextRotation(d)<90?radius / 3 * d.depth:radius/3*d.depth*-1; })	
                .attr("dx", function(d) { return computeTextRotation(d)<90?"6":"-6"}) // margin
                  .attr("dy", ".35em") // vertical-align       
                  .text(function(d) {
                      var nameholder = null;
                      var getWidth = radius/3 * .1;
                      if (d.name.length > (getWidth)) {
                          nameholder = d.name.substring(0,(getWidth)) + "...";
                      }
                      else nameholder = d.name;
                    return nameholder;})
                        //.text(function(d,i) {return d.name})
   
            function zoomIn(p, scope) {

                if (p.depth > 1) p = p.parent;        
                if (!p.children) {
                    //call controller function to make audit call
                    sendAudit(p.key);
                }
                zoom(p, p);
            }

            function zoomOut(p) {
                if (!p.parent) return;
                zoom(p.parent, p);
            }

          // Zoom to the specified new root.
          function zoom(root, p) {
            if (document.documentElement.__transition__) return;

            // Rescale outside angles to match the new layout.
            var enterArc,
                exitArc,
                outsideAngle = d3.scale.linear().domain([0, 2 * Math.PI]);

            function insideArc(d) {
              return p.key > d.key
                  ? {depth: d.depth - 1, x: 0, dx: 0} : p.key < d.key
                  ? {depth: d.depth - 1, x: 2 * Math.PI, dx: 0}
                  : {depth: 0, x: 0, dx: 2 * Math.PI};
            }

            function outsideArc(d) {
              return {depth: d.depth + 1, x: outsideAngle(d.x), dx: outsideAngle(d.x + d.dx) - outsideAngle(d.x)};
            }

            center.datum(root);

            // When zooming in, arcs enter from the outside and exit to the inside.
            // Entering outside arcs start from the old layout.
            if (root === p) enterArc = outsideArc, exitArc = insideArc, outsideAngle.range([p.x, p.x + p.dx]);

                 var new_data=partition.nodes(root).slice(1)

            path = path.data(new_data, function(d) { return d.key; });

                 // When zooming out, arcs enter from the inside and exit to the outside.
            // Exiting outside arcs transition to the new layout.
            if (root !== p) enterArc = insideArc, exitArc = outsideArc, outsideAngle.range([p.x, p.x + p.dx]);

            d3.transition().duration(d3.event.altKey ? 7500 : 750).each(function() {
              path.exit().transition()
                  .style("fill-opacity", function(d) { return d.depth === 1 + (root === p) ? 1 : 0; })
                  .attrTween("d", function(d) { return arcTween.call(this, exitArc(d)); })
                  .remove();

              path.enter().append("path")
                  .style("fill-opacity", function(d) { return d.depth === 2 - (root === p) ? 1 : 0; })
                  .style("fill", function(d) { return d.fill; })
                  .on("click", function(d){zoomIn(d, scope);})
                                 .on("mouseover", mouseOverArc)
                 .on("mousemove", mouseMoveArc)
                 .on("mouseout", mouseOutArc)
                  .each(function(d) { this._current = enterArc(d); });


              path.transition()
                  .style("fill-opacity", 1)
                  .attrTween("d", function(d) { return arcTween.call(this, updateArc(d)); });
            });
            texts = texts.data(new_data, function(d) { return d.key; })
            texts.exit()
                .remove()    
            texts.enter()
                .append("text")

            texts.style("opacity", 0)
                .attr("transform", function(d) { return computeTextRotation(d)<90?"rotate(" + computeTextRotation(d) + ")":"rotate(" + computeTextRotation(d) + ")rotate(-180)"; })
                .attr("text-anchor", function(d){return computeTextRotation(d)<90? "start":"end";})
                .attr("x", function(d) { return computeTextRotation(d)<90?radius / 3 * d.depth:radius/3*d.depth*-1; })	
                .attr("dx", function(d) { return computeTextRotation(d)<90?"6":"-6"}) // margin
                .attr("dy", ".35em") // vertical-align  	
                .text(function(d) {
                    var nameholder = null;
                    var getWidth = radius/3 * .1;
                    if (d.name.length > (getWidth)) {
                        nameholder = d.name.substring(0,(getWidth)) + "...";
                    }
                    else nameholder = d.name;
                    return nameholder;})
                .transition().delay(750).style("opacity", 1)
            }
        };
        function key(d) {
          var k = [], p = d;
          while (p.depth) k.push(p.name), p = p.parent;
          return k.reverse().join(".");
        };
        function fill(d) {
          var p = d;
          while (p.depth > 1) p = p.parent;
          var c = d3.lab(hue(p.name));
          c.l = luminance(d.sum);
          return c;
        };
        function arcTween(b) {
          var i = d3.interpolate(this._current, b);
          this._current = i(0);
          return function(t) {
            return arc(i(t));
          };
        };
        function updateArc(d) {
          return {depth: d.depth, x: d.x, dx: d.dx};
        };
        function sendAudit(interface){              //sends audits directly instead of through controller function
            //scope.getAuditsForInterface(p.key);
            var keys = interface.split('.');
            var interfaceQuery = '{"transactionType":"'+keys[0]+'","application":"'+keys[1]+'","interface1":"'+keys[2]+'","envid":"'+scope.env.dbName+'","timestamp":{"$gte":{"$date":"'+scope.fromDate+'"},"$lt":{"$date":"'+scope.toDate+'"}},"$and":[{"severity":{"$ne":"null"}},{"severity":{"$exists":"true","$ne":""}}]}';

            scope.auditQuery.query(interfaceQuery);
            scope.$apply($location.path("/audits"));
                return;
        }
        d3.select(self.frameElement).style("height", margin.top + margin.bottom + "px");

        createSunburst(data, scope);
    }
    function link(scope, element){
        scope.$watch('sunburstPromise', function(){
            scope.sunburstPromise.then(function(data){
                if(data.data._size === 0){
                    sunburstChart(0, element, scope);
                }
                else {
                    scope.errorMsg = ""
                    var temp = {"_embedded":{"rh:doc":[{"children":[]}]}};
                    temp._embedded['rh:doc'].children = data.data._embedded['rh:doc'];
                    sunburstChart(temp._embedded['rh:doc'], element,scope);
                }
            });
        });
//        $(window).resize(function(){
//            updateSize(scope.sunburstSaver.resizeTemp, element, scope)
//        })
    }
    return{
        link: link,
        controller: 'sunburstController'
    };
});
