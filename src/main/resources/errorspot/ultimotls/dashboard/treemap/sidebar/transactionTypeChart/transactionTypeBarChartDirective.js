var transactionTypeBarChartDirectiveModule = angular.module('transactionTypeBarChartDirectiveModule', ['transactionTypeBarChartControllerModule']);

transactionTypeBarChartDirectiveModule.directive('transactionTypeBarChart',['queryFilter', function(queryFilter){
    function updateSize(data){
        var width = document.getElementById('transactionTypeBarChartDiv').offsetWidth*.9, height = (window.innerHeight*.30);
        if (data === 0){
            d3.select("#transactionTypeBarChart").select("svg").remove();
            var svg = d3.select("#transactionTypeBarChart").append("svg")
                .attr("id", "transactionTypeDiv")
                .attr("width", width)
                .attr("height", height)
                .append("g")
                .attr("transform", "translate(" + width*.065 + "," + height*.5 + ")")
                .append("text").text("No Data Available");
            return;
        }
        barChart(data, "updateChart");
        return;
    };
    function onSelection(d,i){
        d3.select("#error").selectAll("path").style("opacity", 1)
        d3.select("#severity").selectAll("path").style("opacity", 1)
        d3.select("#transactionType").selectAll("rect").style("opacity", 1)
        
        d3.select("#error").selectAll("path").style("opacity", 0.3)
        d3.select("#severity").selectAll("path").style("opacity", 0.3)
        d3.select("#transactionType").selectAll("rect").style("opacity", 0.3)
        
        d3.select("#transactionTypeBar"+i).style("opacity",1)
    }
    function barChart(data, status){
        if(data.length){
            var dynamicSize = data.length/5;
            var adjustedWidth = dynamicSize *.5 + 1;
        }
        var width = document.getElementById('transactionTypeBarChartDiv').offsetWidth*adjustedWidth, height = (window.innerHeight*.28);
        var width2 = document.getElementById('transactionTypeBarChartDiv').offsetWidth;
        var color = d3.scale.category10();
        var barChart = {};
        function upDateTreemap(filterCriteria){
            queryFilter.appendQuery("transactionType",filterCriteria._id);
            queryFilter.broadcast();
        };
        barChart.createChart = function(data){
            var x = d3.scale.ordinal().rangeRoundBands([0, width*.95], .1);
            var y = d3.scale.linear().range([height*.82,0]);
            var xAxis = d3.svg.axis()
                .scale(x)
                .orient("bottom");
            x.domain(//sort by descending order
                data.sort(function(a,b){return b.count - a.count})
                    .map(function(d){return d._id;}))
                    .copy();
            y.domain([0,d3.max(data, function(d) { return d.count; })]);
            var yAxis = d3.svg.axis()
                .scale(y)
                .orient("left")
                .ticks(5, "");
            var svg = d3.select("#transactionType")
                .attr("width", width)
                .attr("height", height)
                .append("g")
                .attr("transform", "translate(" + width2*.1+ "," + height*.1 + ")");
            
            
            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height*.82 + ")")
                .call(xAxis);
//}); 
            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
              .append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", 2)
                .attr("dy", ".71em")
                .style("text-anchor", "end")
                .text("Count");
      
            svg.selectAll(".bar").data(data)
                .enter().append("rect")
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i)})
                .style("fill", function(d,i){return color(i);})
                .attr("class", "bar").attr("id", function(d,i){return "transactionTypeBar"+i;})
                //.attr("x", function(d) { return x(d._id)+5; })
                .attr("x", function(d){return x(d._id)+5;})
                .attr("width", x.rangeBand())
                .transition()
                .delay(function(d,i){return i*100;})
                .attr("y", function(d,i) { return y(d.count); })
                .attr("height", function(d) { return (height*.82 - y(d.count)); });
        };
        if(status === "updateChart"){
            d3.select("#transactionTypeBarChart").select("svg").remove();
            var svg = d3.select("#transactionTypeBarChart").append("svg").attr("width",width).attr("height",height).attr("id", "transactionTypeDiv");
            svg.append("g").attr("id","transactionType")
                .append("text").attr("transform", "translate(0,15)").text("Transaction Type Chart");
            barChart.createChart(data);
            return;
        };
        if(status === "no_data"){ //Will append a Message for no data and return out of the function
            d3.select("#transactionTypeBarChart").select("svg").remove();
            var svg = d3.select("#transactionTypeBarChart").append("svg")
                .attr("id", "transactionTypeDiv").attr("width", width2).attr("height", height)
                .append("g").attr("transform", "translate(" + width2*.065 + "," + height*.5 + ")")
                .append("text").text("No Data Available");
            return;
        };
        if(status === "createChart"){
            d3.select("#transactionTypeBarChart").select("svg").remove();
            var svg = d3.select("#transactionTypeBarChart").append("svg").attr("width",width).attr("height",height).attr("id", "transactionTypeDiv");
            svg.append("g").attr("id","transactionType");
            svg.append("text").attr("transform", "translate(0,15)").text("Transaction Type Chart");
            barChart.createChart(data);
        };
    };
    function link(scope){
        scope.$watch('transactionTypeBarChartPromise', function(){
            scope.transactionTypeBarChartPromise.then(function(getCall){ //handles the promise
                if(getCall.data._size === 0){
                    scope.transactionTypeTempData = 0;
                    barChart(0, "no_data");
                    return;
                }
                var temp = getCall.data._embedded['rh:doc'];
                scope.transactionTypeTempData = temp;
                barChart(temp, "createChart");
            });
            $(window).resize(function(){
                updateSize(scope.transactionTypeTempData);
            })
        });
    };
    return{
        restrict: 'E',
        link: link,
        controller: 'transactionTypeBarChartController'
    };
}]);