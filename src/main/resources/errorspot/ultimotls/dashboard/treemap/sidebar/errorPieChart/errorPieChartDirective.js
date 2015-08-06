var errorPieChartDirectiveModule = angular.module('errorPieChartDirectiveModule', ['errorPieChartControllerModule']);

errorPieChartDirectiveModule.directive('errorPieChart',['queryFilter', function(queryFilter){
    var initialHeight = document.getElementById("row2").offsetHeight;
    function updateSize(data){
        var width = document.getElementById('errorTypePieChartDiv').offsetWidth, height = (window.innerHeight*.28);
        if (data === 0){ //Will append a Message for no data and return out of the function
            d3.select("#errorTypePieChart").select("svg").remove();
            var svg = d3.select("#errorTypePieChart").append("svg")
                .attr("width", width).attr("height", height)
                .append("g").attr("transform","translate("+width*.13+","+height*.5+")");
            svg.append("text").text("No Data Available");
          return;
        };
        pieChart(data,"updateChart");
        return;
    }
    function cleanUp(){
        d3.select("#errorTypePieChartDiv").select("#reset").remove();
        d3.select("#severityPieChartDiv").select("#reset").remove();
        d3.select("#transactionTypeBarChartDiv").select("#reset").remove();
    }
    function onReset(){
        cleanUp();
        d3.select("#severity").selectAll("path").style("opacity", 1);
        d3.select("#error").selectAll("path").style("opacity", 1);
        d3.select("#transactionType").selectAll("rect").style("opacity",1);
        queryFilter.appendQuery("","");
        queryFilter.broadcast();
    }
    function onSelection(d,i){
        var width = document.getElementById('errorTypePieChartDiv').offsetWidth;
        cleanUp();
        d3.select("#error").selectAll("path").style("opacity", 0.3);
        d3.select("#severity").selectAll("path").style("opacity", 0.3);
        d3.select("#transactionType").selectAll("rect").style("opacity", 0.3);
        
        d3.select("#errorInnerSlice"+i).style("opacity",1);
        d3.select("#errorTopSlice"+i).style("opacity",1);
        d3.select("#errorOuterSlice"+i).style("opacity",1);
        
        d3.select("#errorTypePieChart").select("svg")
            .append("g").attr("transform", "translate("+width*.7+",15)")
            .attr("id","reset").on("click", function(d){onReset();})
            .append("text").text("Reset");
    }
    function pieChart(data, status){
        var Donut3D = {};
        var color = d3.scale.category10();
        var width = document.getElementById('errorTypePieChartDiv').offsetWidth, height = (window.innerHeight*.28);
        var centerX = width*.5, centerY = height*.45, radiusX = centerX*.7, radiusY = centerY*.66, pieHeight = centerY*.2, innerRadius = .3;
        function upDateTreemap(filterCriteria){
            queryFilter.appendQuery("errorType",filterCriteria.data._id);
            queryFilter.broadcast();
        };
        function pieTop(d, rx, ry, ir){
            if(d.endAngle - d.startAngle == 0 ) return "M 0 0";
            var sx = rx*Math.cos(d.startAngle),
                sy = ry*Math.sin(d.startAngle),
                ex = rx*Math.cos(d.endAngle),
                ey = ry*Math.sin(d.endAngle);
            var ret =[];
            ret.push("M",sx,sy,"A",rx,ry,"0",(d.endAngle-d.startAngle > Math.PI? 1: 0),"1",ex,ey,"L",ir*ex,ir*ey);
            ret.push("A",ir*rx,ir*ry,"0",(d.endAngle-d.startAngle > Math.PI? 1: 0), "0",ir*sx,ir*sy,"z");
            return ret.join(" ");
        };
        function pieOuter(d, rx, ry, h ){
            var startAngle = (d.startAngle > Math.PI ? Math.PI : d.startAngle);
            var endAngle = (d.endAngle > Math.PI ? Math.PI : d.endAngle);
            var sx = rx*Math.cos(startAngle),
                sy = ry*Math.sin(startAngle),
                ex = rx*Math.cos(endAngle),
                ey = ry*Math.sin(endAngle);
            var ret =[];
            ret.push("M",sx,h+sy,"A",rx,ry,"0 0 1",ex,h+ey,"L",ex,ey,"A",rx,ry,"0 0 0",sx,sy,"z");
            return ret.join(" ");
	};
        function pieInner(d, rx, ry, h, ir ){
            var startAngle = (d.startAngle < Math.PI ? Math.PI : d.startAngle);
            var endAngle = (d.endAngle < Math.PI ? Math.PI : d.endAngle);
            var sx = ir*rx*Math.cos(startAngle),
                sy = ir*ry*Math.sin(startAngle),
                ex = ir*rx*Math.cos(endAngle),
                ey = ir*ry*Math.sin(endAngle);
            var ret =[];
            ret.push("M",sx, sy,"A",ir*rx,ir*ry,"0 0 1",ex,ey, "L",ex,h+ey,"A",ir*rx, ir*ry,"0 0 0",sx,h+sy,"z");
            return ret.join(" ");
	};
        function fittedText(d){
            var angle = Math.abs(d.endAngle - d.startAngle);
            return angle;
        }
        Donut3D.draw=function(id, data, x /*center x*/, y/*center y*/, 
			rx/*radius x*/, ry/*radius y*/, h/*height*/, ir/*inner radius*/){
            function mouseOverSlice(d) {
               d3.select(this).attr("stroke","black");
               tooltip.html(d.data._id);
               return tooltip.transition().duration(50).style("opacity", 0.9);
            };
            function mouseOutSlice(){
               d3.select(this).attr("stroke","");
               return tooltip.style("opacity", 0);
            };
            function mouseMoveSlice () {
               return tooltip
               .style("top", (d3.event.pageY - 15)-
               ((document.getElementById("row2").offsetHeight - initialHeight)*.8)+"px")
               .style("left", (d3.event.pageX + 15)+"px");
            };

            var _data = d3.layout.pie().sort(function(a,b){return b.count - a.count}).value(function(d) {return d.count;})(data);
            //d3.select("html").selectAll("*:not(svg)").on("mouseover",mouseOutSlice);//helps remove tooltip
            var slices = d3.select("#"+id).append("g").attr("transform", "translate(" + x + "," + y + ")")
                .attr("class", "slices");
            var tooltip = d3.select("#errorTypePieChart").append("div").attr("id", "tooltip")
                .style("position", "fixed").style("opacity", 0).style("z-index", 1000);	
            slices.selectAll(".innerSlice").data(_data).enter().append("path").attr("class","innerSlice")
                .attr("id", function(d,i){return "errorInnerSlice"+i})
                .style("fill", function(d,i){return color(i);})
                .style("stroke", "rgb(87, 87, 87)")
                .attr("d",function(d){ return pieInner(d, rx+0.5,ry+0.5, h, ir);})
                .on("mouseover", mouseOverSlice)
                .on("mousemove", mouseMoveSlice)
                .on("mouseout", mouseOutSlice)
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i);})
                .each(function(d){this._current=d;});
            slices.selectAll(".topSlice").data(_data).enter().append("path").attr("class", "topSlice")
                .attr("id", function(d,i){return "errorTopSlice"+i})
                .style("fill", function(d,i){return color(i);})
                .style("stroke", "rgb(87, 87, 87)")
                .attr("d",function(d){ return pieTop(d, rx, ry, ir);})
                .on("mouseover", mouseOverSlice)
                .on("mousemove", mouseMoveSlice)
                .on("mouseout", mouseOutSlice)
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i);})
                .each(function(d){this._current=d;});
            slices.selectAll(".outerSlice").data(_data).enter().append("path").attr("class", "outerSlice")
                .attr("id", function(d,i){return "errorOuterSlice"+i})
                .style("fill", function(d,i){return color(i);})
                .style("stroke", "rgb(87, 87, 87)")
                .attr("d",function(d){ return pieOuter(d, rx-.5,ry-.5, h);})
                .on("mouseover", mouseOverSlice)
                .on("mousemove", mouseMoveSlice)
                .on("mouseout", mouseOutSlice)
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i);})
                .each(function(d){this._current=d;});
            slices.selectAll(".label").data(_data).enter().append("text").attr("class", "label")
                .attr("x",function(d){ return .7*rx*Math.cos(0.5*(d.startAngle+d.endAngle));})
                .attr("y",function(d){ return 0.6*ry*Math.sin(0.5*(d.startAngle+d.endAngle));})
                .attr("dx",-12)
                .on("mouseover", mouseOverSlice)
                .on("mousemove", mouseMoveSlice)
                .on("mouseout", mouseOutSlice)
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i);})
                .style("fill", "black")
                .style("font-size", "11px")
                .text(function(d){return fittedText(d)<.4?"":d.data._id})
                .each(function(d){this._current=d;});				
	};
        this.Donut3D = Donut3D;
        if (status === "no_data"){ //Will append a Message for no data and return out of the function
            d3.select("#errorTypePieChart").select("svg").remove();
            d3.select("#errorTypePieChart").select("div").remove();
            var svg = d3.select("#errorTypePieChart").append("svg")
                .attr("width", width).attr("height", height).append("g")
                .attr("transform", "translate(" + width*.13 + "," + height*.5 + ")");
            svg.append("text").text("No Data Available");
            return;
        };
        if(status === "updateChart"){
            d3.select("#errorTypePieChart").select("svg").remove();
            d3.select("#errorTypePieChart").select("div").remove();
            var svg = d3.select("#errorTypePieChart").append("svg").attr("width",width).attr("height",height);
            svg.append("g").attr("id","error").append("text").attr("transform", "translate(0,15)").text("Error Type Chart");
            Donut3D.draw("error",data,centerX,centerY,radiusX,radiusY,pieHeight,innerRadius);
            return;
        };
        if(status === "createChart"){
            d3.select("#errorTypePieChart").select("svg").remove();
            d3.select("#errorTypePieChart").select("div").remove();
            var svg = d3.select("#errorTypePieChart").append("svg").attr("width",width).attr("height",height);
            svg.append("g").attr("id","error");
            svg.append("text").attr("transform", "translate(0,15)").text("Error Type Chart");
            Donut3D.draw("error",data,centerX,centerY,radiusX,radiusY,pieHeight,innerRadius);
            return;
        };
    };
    function link(scope){
        scope.$watch('errorPieChartPromise', function(){
            scope.errorPieChartPromise.then(function(getCall){ //handles the promise\
                if(getCall.data._size === 0){
                    scope.errorTempData = 0;
                    pieChart(0,"no_data");
                    return;
                }
                var temp = getCall.data._embedded['rh:doc'];
                scope.errorTempData = temp;
                pieChart(temp,"createChart");
            });
            $(window).resize(function(){
               updateSize(scope.errorTempData);
            });
        });
    };
    return{
        restrict: 'E',
        link: link,
        controller: 'errorPieChartController'
    };
}]);