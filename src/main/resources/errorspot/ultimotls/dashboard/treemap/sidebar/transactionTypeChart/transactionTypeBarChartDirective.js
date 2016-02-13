var transactionTypeBarChartDirectiveModule = angular.module('transactionTypeBarChartDirectiveModule', ['transactionTypeBarChartControllerModule']);

transactionTypeBarChartDirectiveModule.directive('transactionTypeBarChart',['queryFilter', function(queryFilter){
    function updateSize(data, scope){
        var width = document.getElementById('transactionTypeBarChartDiv').offsetWidth*.9, height = (window.innerHeight*.29);
        if (data === 0){
            d3.select("#transactionTypeBarChart").select("svg").remove();
            d3.select("#transactionTypeBarChart").append("svg")
                .attr("id", "transactionTypeDiv")
                .attr("width", width).attr("height", height).append("g")
                .attr("transform", "translate(" + width*.065 + "," + height*.5 + ")")
                .append("text").text("No Data Available");
            return;
        }
        barChart(data, "updateChart", scope);
        return;
    };
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
        d3.selectAll(".transactionTypeText").style("opacity", 1);
        d3.selectAll(".transactionTypeText").selectAll("text").style("opacity", 1);
        queryFilter.appendQuery("","");
        queryFilter.broadcast();
    }
    function onSelection(d,i){
        var width = document.getElementById('transactionTypeBarChartDiv').offsetWidth;
        cleanUp();
        d3.select("#error").selectAll("path").style("opacity", 0.3);
        d3.select("#severity").selectAll("path").style("opacity", 0.3);
        d3.select("#transactionType").selectAll("rect").style("opacity", 0.3);
        d3.selectAll(".transactionTypeText").style("opacity", 0.3);
        d3.select("#transactionTypeBar"+i).style("opacity",1);
        d3.select("#transactionTypeText"+i).style("opacity",1);
        var svg = d3.select("#transactionTypeBarChart").select("svg").append("g")
            .attr("transform", "translate("+width*.7+",15)")
            .attr("id","reset")
            .on("click", function(d){onReset();});
        svg.append("text").text("Reset");
    }
    function barChart(data, status, scope){
        
        var width = document.getElementById('transactionTypeBarChartDiv').offsetWidth, height = (window.innerHeight*.30);
        var width2 = document.getElementById('transactionTypeBarChartDiv').offsetWidth;
        var color = d3.scale.category10();
        var pageCount = 0;
        var barChart = {};
        if(data !== undefined && data.length > 0){
            var pages = Math.ceil(data.length/10);
            var dataHolder = [];
            dataHolder.push(data.sort(function(a, b){return b.count-a.count;}));
            var slicedData = dataHolder[0].slice(0,9);
            console.log(slicedData);
            d3.select("#transactionTypeDiv").append("text")
                .attr("transform", "translate("+height*.83+","+width*60+")").text("Next");
        }
        
        
        function upDateTreemap(filterCriteria){
            if(typeof filterCriteria  === "string"){
                queryFilter.appendQuery("transactionType",filterCriteria);
                queryFilter.broadcast();
                return;
            }
            queryFilter.appendQuery("transactionType",filterCriteria._id);
            queryFilter.broadcast();
        };
        function pagination(direction){
            if(direction === "next"){
                if(pageCount >= pages-1){
                    return;
                }
                pageCount++;
                d3.select("#transactionType").selectAll("g").remove();
                slicedData = dataHolder[0].slice(10*pageCount,10*pageCount+9);
                barChart.createHorizontal(slicedData);
                
            }
            if(direction === "previous"){
                if(pageCount < 1){
                    return;
                }
                pageCount--;
                d3.select("#transactionType").selectAll("g").remove();
                slicedData = dataHolder[0].slice(10*pageCount,10*pageCount+9);
                barChart.createHorizontal(slicedData);
                
            }
        }
        barChart.createHorizontal = function(data){
            d3.select("#transactionTypePrevious").style("opacity", 1);
            d3.select("#transactionTypeNext").style("opacity", 1);
            
            if(pageCount === 0){
                d3.select("#transactionTypePrevious").style("opacity", 0.3);
            }
            if(pageCount === pages-1){
                d3.select("#transactionTypeNext").style("opacity", 0.3);
            }
            if(scope.treemapSaver.saveScale === undefined){
                scope.treemapSaver.saveScale = d3.scale.linear().range([0,width*.65])
                    .domain([0,d3.max(data,function(d){return d.count;})]);
            }
            if(d3.select("#transactionTypePrevious").style("opacity") !== "1"){
                scope.treemapSaver.saveScale = d3.scale.linear().range([0,width*.65])
                    .domain([0,d3.max(data,function(d){return d.count;})]);
            }
            var x = scope.treemapSaver.saveScale;
            var y = d3.scale.ordinal().rangeRoundBands([0,height*.70],.1)
                .domain(data.sort(function(a,b){
                    return b.count-a.count;})
                .map(function(d){return d._id;})
            ).copy();
            var xAxis = d3.svg.axis().scale(x).orient("top").ticks(5,"");
            var yAxis = d3.svg.axis().scale(y).orient("left");
            var svg = d3.select("#transactionType")
                .attr("width", width).attr("height", height).append("g")
                .attr("transform", "translate(" + width2*.15+ "," + height*.13 + ")");
            var labels = svg.append("g")
                .attr("class","y axis").attr("transform", "translate("+width2*.15+",15)")
                .call(yAxis);
            labels.selectAll(".tick")
                .attr("id",function(d,i){return "transactionTypeText"+i;})
                .attr("class","transactionTypeText")
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i)});
            svg.append("g").attr("class","x axis").call(xAxis)
                .attr("transform", "translate("+width2*.15+",10)")
                .append("text").attr("x", -20).attr("dx", ".71em").attr("y", -10)
                .style("text-anchor","end").text("Count");
            svg.selectAll(".bar").data(data)
                .enter().append("rect")
                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i)})
                .style("fill", function(d,i){return color(i);})
                .attr("class", "bar").attr("id", function(d,i){return "transactionTypeBar"+i;})
                .attr("y", function(d){return y(d._id);})
                .attr("width", function(d){return x(d.count);})
                .transition().delay(function(d,i){return i*100;})
                .attr("height", function(d){return y.rangeBand();})
                .attr("transform","translate("+width2*.15+",15)");
        }
//        barChart.createVertical = function(data){
//            var x = d3.scale.ordinal().rangeRoundBands([0, width*.95], .1);
//            var y = d3.scale.linear().range([height*.82,0]);
//            var xAxis = d3.svg.axis().scale(x).orient("bottom");
//            x.domain(data.sort(function(a,b){return b.count - a.count})
//                .map(function(d){return d._id;})
//            ).copy();
//            y.domain([0,d3.max(data, function(d) { return d.count;})]);
//            var yAxis = d3.svg.axis().scale(y).orient("left").ticks(5,"");
//            var svg = d3.select("#transactionType")
//                .attr("width", width).attr("height", height).append("g")
//                .attr("transform", "translate(" + width2*.1+ "," + height*.1 + ")");
//            svg.append("g")
//                .attr("class", "x axis").attr("transform", "translate(0," + height*.82 + ")")
//                .call(xAxis);
//            svg.append("g").attr("class", "y axis").call(yAxis)
//                .append("text").attr("transform", "rotate(-90)")
//                .attr("y", 2).attr("dy", ".71em")
//                .style("text-anchor", "end").text("Count");
//            svg.selectAll(".bar").data(data)
//                .enter().append("rect")
//                .on("click", function(d,i){upDateTreemap(d);onSelection(d,i)})
//                .style("fill", function(d,i){return color(i);})
//                .attr("class", "bar").attr("id", function(d,i){return "transactionTypeBar"+i;})
//                .attr("x", function(d){return x(d._id)+5;})
//                .attr("width", x.rangeBand())
//                .transition().delay(function(d,i){return i*100;})
//                .attr("y", function(d){return y(d.count);})
//                .attr("height", function(d){return (height*.82 - y(d.count));});
//            };
        if(status === "updateChart"){
            d3.select("#transactionTypeBarChart").select("svg").remove();
            var svg = d3.select("#transactionTypeBarChart").append("svg").attr("width",width).attr("height",height).attr("id", "transactionTypeDiv");
            svg.append("g").attr("id","transactionType")
                .append("text").attr("transform", "translate(0,15)").text("Transaction Type Chart");
            svg.append("text").attr("id","transactionTypeNext").on("click", function(d){pagination("next")})
                    .attr("transform", "translate("+width*.73+","+height*.965+")").text("Next");
            svg.append("text").attr("id","transactionTypePrevious").on("click", function(d){pagination("previous")})
                    .attr("transform", "translate("+width*.55+","+height*.965+")").text("Previous");
            barChart.createHorizontal(slicedData);
            return;
        };
        if(status === "no_data"){ //Will append a Message for no data and return out of the function
            d3.select("#transactionTypeBarChart").select("svg").remove();
            d3.select("#transactionTypeBarChart").append("svg")
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
            svg.append("text").attr("id","transactionTypeNext").on("click",function(d){pagination("next")})
                    .attr("transform", "translate("+width*.75+","+height*.965+")").text("Next");
            svg.append("text").attr("id","transactionTypePrevious").on("click",function(d){pagination("previous")})
                    .attr("transform", "translate("+width*.55+","+height*.965+")").text("Previous");
            barChart.createHorizontal(slicedData);
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
                barChart(temp, "createChart", scope);
            });
            $(window).resize(function(){
                updateSize(scope.transactionTypeTempData, scope);
            })
        });
    };
    return{
        restrict: 'E',
        link: link,
        controller: 'transactionTypeBarChartController'
    };
}]);