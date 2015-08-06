var appDirectives = angular.module('myDirectives', ['ultimotls']);





appDirectives.directive('severityPieChart', ['$http', function($http){
    function severityPieChart(data, element){
        var ele = element[0];
        var svg = d3.select(ele)
                .append("svg")
                .append("g")
        svg.append("g")
                .attr("class","slices");
        svg.append("g")        
                .attr("class","labels");
        svg.append("g")
                .attr("class","lines");
        var width = (window.innerWidth*.43), height = (window.innerHeight*.43);
        radius = Math.min(width, height) / 2;

        var pie = d3.layout.pie()
                .sort(null)
                .value(function(d){
                    return d.value;
                });
        var arc = d3.svg.arc()
                .outerRadius(radius * 0.5)
                .innerRadius(radius * 0.3);
        var outerArc = d3.svg.arc()
                .outerRadius(radius * 0.50)
                .innerRadius(radius * 0.50);

        svg.attr("transform", "translate(" + width/2 +","+ height/2+ ")" );
        var key = function(d){
            return d.data.label;
        };
        function randomColorGen(){
            var color = Math.floor(Math.random()*Math.pow(256,3)).toString(16);
            while(color.length < 6) {
                color = "0"+color;
            }
            return "#"+color;
        }

        var color = d3.scale.ordinal();
        var temp = [];
        var colorTemp = [];
        for (var i = 0; i < data.length; i++) {
            temp.push(data[i]._id);
            colorTemp.push(randomColorGen());
        }
        color.domain(temp)
                .range(colorTemp);
        function aggregateData(data) {
            var labels = color.domain();
            var i = -1;
            return labels.map(function(label){
                i++;
                return{
                    label: label, value: data[i].count
                }
            });
        }
        function change(data) {
            var slice = svg.select(".slices")
                .selectAll("path.slice")
                .data(pie(data), key);
            slice.enter()
                .insert("path")
                .style("fill", function(d){return color(d.data.label);})
                .attr("class", "slice");
            slice.transition().duration(1000)
                .attrTween("d",function(d){
                    this._current = this._current || d;
                    var interpolate = d3.interpolate(this._current, d);
                    this._current = interpolate(0);
                    return function(t) {
                        return arc(interpolate(t));
                    };
            })
            slice.exit()
                .remove();

        //////TEXT LABELS/////
        var text = svg.select(".labels").selectAll("text")
            .data(pie(data), key);
        text.enter()
            .append("text")
            .attr("dy", ".35em")
            .text(function(d) {
                return d.data.label +" "+ d.data.value;
        });

        function midAngle(d) {
            return d.startAngle + (d.endAngle - d.startAngle)/2;
        }

        text.transition().duration(1000)
            .attrTween("transform", function(d){
                this._current = this._current || d;
                var interpolate = d3.interpolate(this._current, d);
                this._current = interpolate(0);
                return function(t) {
                    var d2 = interpolate(t);
                    var pos = outerArc.centroid(d2);
                    pos[0] = radius *(midAngle(d2) < Math.PI ? 1:-1); //Not Sure what this is for
                    return "translate("+ pos +")";
                };
            })
            .styleTween("text-ancho", function(d){
                this._current = this._current || d;
                var interpolate = d3.interpolate(this._current, d);
                this._current = interpolate(0);
                return function(t){
                    var d2 = interpolate(t);
                    return midAngle(d2) < Math.PI ? "start" : "end";
                };
            });
        text.exit()
            .remove();

        /////////SLICE TO TEXT POLYLINES///////////
        var polyline = svg.select(".lines").selectAll("polyline").data(pie(data), key);
        polyline.enter().append("polyline");
        polyline.transition().duration(1000)
                .attrTween("points", function(d){
                this._current = this._current || d;
                var interpolate = d3.interpolate(this._current,d);
                this._current = interpolate(0);
                return function(t) {
                    var d2 = interpolate(t)
                    var pos = outerArc.centroid(d2);
                    pos[0] = radius * 0.8 * (midAngle(d2) < Math.PI ? 1 : -1);
                    return [arc.centroid(d2), outerArc.centroid(d2), pos];
                };
            });
        polyline.exit()
                .remove();

        }
        change(aggregateData(data));
    }
    function link(scope, element){
        /*scope.dataPayload.then(function(d){
            calledData['audits'] = d.data._embedded['rh:doc'];
            severityPieChart(scope.pieQueryData(currentCriteria, calledData, scope), element);
        })*/
            severityPieChart(scope.severityPieChartQueriedData, element);
            scope.$watch('severityPieChartQueriedData', function(){
                scope.replaceGraph("severityPieChart", scope.severityPieChartQueriedData, element, severityPieChart);
            })
    };
    return{
        link: link,
        controller: "Dashboard"
    };
}]);
appDirectives.directive('appBarChart', function(){ 
    function createBarChart(data, element){
        var ele = element[0];
        var margin = {top: 30, right: 20, bottom: 0, left: 40};
        var width = window.innerWidth*.45 - margin.left - margin.right;
        var height = window.innerHeight*.30 - margin.top - margin.bottom;
        var x = d3.scale.ordinal()
            .rangeRoundBands([0, width], .1);
        var y = d3.scale.linear()
            .range([height, 0]);
        var xAxis = d3.svg.axis()
            .scale(x)
            .orient("bottom");

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left")
            .ticks(5, "");

        var svg = d3.select(ele).append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            x.domain(data.map(function(d) { return d._id; }));
            y.domain([0, d3.max(data, function(d) { return d.count; })]);
//scope.$watch('data', function(data){              
            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis);
//}); 
            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
              .append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", 6)
                .attr("dy", ".71em")
                .style("text-anchor", "end")
                .text("Frequency");

            svg.selectAll(".numLabel")
              .data(data)
            .enter().append("text")
              .attr("class", "numLabel")
              .attr("x", function(d) { return x(d._id) + (x.rangeBand()/2)-10; })
              .attr("y", function(d){return y(d.count)-5;})
              .text(function(d) { return d.count; });

        svg.selectAll(".bar")
                .data(data)
            .enter().append("rect")
                .attr("class", "bar")
                //.attr("x", function(d) { return x(d._id)+5; })
                .attr("x", function(d){return x(d._id)+5;})
                .attr("width", x.rangeBand())
                .transition()
                .delay(function(d,i){return i*300;})
                .attr("y", function(d,i) { return y(d.count); })
                .attr("height", function(d) { return (height - y(d.count)); });
                //.on("click",test);
        //Dynamic sizing
        /*scope.$watch(function(){
            return window.innerWidth;
        }, function(){
            width = window.innerWidth*.45 - margin.left - margin.right;
            xAxis.scale(x);
        })*/
    }
    function link(scope, element){
        createBarChart(scope.appBarChartQueriedData, element);
        scope.$watch('appBarChartQueriedData',function(){
            scope.replaceGraph("appBarChart", scope.appBarChartQueriedData, element, createBarChart);
        });
    };
    return{
        link: link,
        controller: "Dashboard"
    };
});
appDirectives.directive('treemapChart', ['$http', function($http){
    function createTree(data, element){
        var treeData = {name:"tree", children:[{}]};
        for(var x=0;x<data.length;x++){
            var name = data[x]._id.replace(/([A-Z][a-z])/g, '$1').replace(/([a-z])([A-Z])/g, '$1 $2');
            treeData.children.push({name:data[x]._id.replace(/([A-Z][a-z])/g, '$1').replace(/([a-z])([A-Z])/g, '$1 $2'),
                size:data[x].count});
        }
        var ele = element[0];
        var width = window.innerWidth*.4,
            height = 225,
            color = d3.scale.category20c(),
            div = d3.select(ele).append("div")
               .style("position", "relative");

        var treemap = d3.layout.treemap()
            .size([width, height])
            .sort(function(a, b) { return a.value - b.value; })
            .value(function(d) { return d.size; });

        var node = div.datum(treeData).selectAll(".node")
                .data(treemap.nodes)
            .enter().append("div")
                .on("click", function(){return console.log("hovering")})
                .attr("class", "node")
                .call(position)
                .style("background-color", function(d) {
                    return d.name === 'tree' ? '#fff' : color(d.name); })
                .append('div')
                .style("font-size", function(d) {
                    // compute font size based on sqrt(area)
                    return Math.max(20, 0.1*Math.sqrt(d.area))+'px'; })
                .text(function(d) { return d.children ? null : d.name; });

        function position() {
            this.style("left", function(d) { return d.x + "px"; })
                .style("top", function(d) { return d.y + "px"; })
                .style("width", function(d) { return Math.max(0, d.dx - 1) + "px"; })
                .style("height", function(d) { return Math.max(0, d.dy - 1) + "px"; });
        }
    }
    function link(scope, element){
        createTree(scope.interfaceChartQueriedData, element);
        scope.$watch('interfaceChartQueriedData',function(){
            scope.replaceGraph("treemapChart", scope.interfaceChartQueriedData, element, createTree);
        });
    };
    return { 
        link: link,
        controller: "Dashboard"
    };
}]);
appDirectives.directive('interfaceStackBarChart', ['$http', function($http){
            function createBarStack(barData, element){
                var ele = element[0];
                var margins = {
                    top: 12,
                    left: 200,
                    right: 24,
                    bottom: 24
                },
                legendPanel = {
                    width: 100
                },
                width = window.innerWidth*.8 - margins.left - margins.right - legendPanel.width,
                    height = (barData.length * 20) - margins.top - margins.bottom;
                    var data2 = [{}];
                    data2.pop();
                    var bigCount = barData[0].count;
            
                    for(var x = 0; x < barData.length; x++){
                        if(barData[x].count > bigCount){
                            bigCount = barData[x].count;
                        }
                    }
                    var dataPush = [];
                    for(var x = 0; x < bigCount; x++){
                        dataPush = [];
                        for(var y = 0; y < barData.length; y++){
                            var setCount = 0;
                            if(barData[y].count > x){
                                setCount = 1;
                            }
                            dataPush.push({month: barData[y]._id.audit_date.month + ", " + barData[y]._id.audit_date.day + ", "
                                        + barData[y]._id.audit_date.year + "   " + barData[y]._id.interface ,count: setCount});
                        }
                        //data2Push += "], name: Series" + (x+1) + "},";
                        data2.push({data:dataPush, name:'Series#1'});
                    }
                    series = data2.map(function (d) {
                        return d.name;
                    });
                    data2 = data2.map(function (d) {
                        return d.data.map(function (o, i) {
                            // Structure it so that your numeric
                            // axis (the stacked amount) is y
                            return {
                                y: o.count,
                                x: o.month
                            };
                        });
                    });
                    stack = d3.layout.stack();
                    stack(data2);

                    var data2 = data2.map(function (group) {
                        return group.map(function (d) {
                            // Invert the x and y values, and y0 becomes x0
                            return {
                                x: d.y,
                                y: d.x,
                                x0: d.y0
                            };
                        });
                    });
                    svg = d3.select(ele)
                        .append('svg')
                        .attr('width', width + margins.left + margins.right + legendPanel.width)
                        .attr('height', height + margins.top + margins.bottom)
                        .append('g')
                        .attr('transform', 'translate(' + margins.left + ',' + margins.top + ')');
                    xMax = d3.max(data2, function (group) {
                        return d3.max(group, function (d) {
                            return d.x + d.x0;
                        });
                    });
                    var xaxis_svg = d3.select("#stackBarChartLabel")
                        .append('svg')
                        .attr("width", width + margins.left + margins.right)
                        .attr("height", margins.bottom);

                    var xScale = d3.scale.linear()
                        .domain([0, xMax])
                        .range([0, width]);
                    months = data2[0].map(function (d) {
                        return d.y;
                    });
                    var yScale = d3.scale.ordinal()
                        .domain(months)
                        .rangeRoundBands([0, height], .1);
                    var xAxis = d3.svg.axis()
                            .ticks(bigCount,"")
                        .scale(xScale)
                        .orient('bottom');
                    var yAxis = d3.svg.axis()
                        .scale(yScale)
                        .orient('left');
                    colours = d3.scale.category10();
                    groups = svg.selectAll('g')
                        .data(data2)
                        .enter()
                        .append('g')
                        .style('fill', function (d, i) {
                        return colours(i);
                    });
                    rects = groups.selectAll('rect')
                        .data(function (d) {
                        return d;
                    })
                        .enter()
                        .append('rect')
                        .attr('x', function (d) {
                        return xScale(d.x0);
                    })
                        .attr('y', function (d, i) {
                        return yScale(d.y);
                    })
                        .attr('height', function (d) {
                        return yScale.rangeBand();
                    })
                        .attr('width', function (d) {
                        return xScale(d.x);
                    })
                    xaxis_svg.append('g')
                        .attr('class', 'axis')
                        .attr('transform', 'translate('+margins.left+',' + 0 + ')')
                        .call(xAxis);
                    
                    svg.append('g')
                        .attr('class', 'axis')
                        .call(yAxis);

                    }
            function link(scope, element){
                var query = jsonPath(scope.calledData, "$.audits[*]");
                var interfaceByDatePieData = [];
                for (var i = 0; i < query.length; i++){
                    var pieEntry =  {"_id":
                                        {"interface":query[i]._id.interface, 
                                         "audit_date":
                                            {"month":query[i]._id.audit_date.month,
                                             "day":query[i]._id.audit_date.day, 
                                             "year":query[i]._id.audit_date.year
                                            }
                                        }, 
                                    "count":query[i].count
                                    };   
                    interfaceByDatePieData.push(pieEntry);
                };
                createBarStack(interfaceByDatePieData, element);
            }
            return { 
                link: link,
                //controller: "Dashboard"
            };
        }]);
appDirectives.controller('Dashboard', ['$scope','$window', 'customService', function($scope, $window, customService){
    $scope.severityPieChartQueriedData = {};
    $scope.calledData =
    {
        "audits":[
        {
            "count":1,
            "_id":{
                "severity":"Error",
                "application":"SOA",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":7
                },
                "interface":"BPELCallEBS"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Fatal",
                "application":"EBS",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":4
                },
                "interface":"CreateOrderPLSQL"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Fatal",
                "application":"SOA",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":12
                },
                "interface":"BPELCreateOrder"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Fatal",
                "application":"SOA",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":11
                },
                "interface":"BPELCallEBS"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Critical",
                "application":"Salesforce",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":14
                },
                "interface":"SyncCustomer"
                }
        },
        {
            "count":1,
            "_id":{
                "severity":"Error",
                "application":"Salesforce",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":12
                },
                "interface":"CustomerCreate"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Critical",
                "application":"EBS",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":13
                },
                "interface":"LookupCustomerPLSQL"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Critical",
                "application":"Salesforce",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":4
                },
                "interface":"CustomerCreate"
            }
        },
        {
            "count":1,
            "_id":{
                "severity":"Critical",
                "application":"SOA",
                "audit_date":{
                    "month":4,
                    "year":2015,
                    "day":15
                },
                "interface":"BPELCreateOrder"
            }
        }
        ]
    };
    $scope.sunburstData = {"_embedded":{"rh:doc":[{"children":[]}]}};
    var currentSeverity = "", currentApplication = "";
    angular.element($window).on('resize', function(){
        $scope.$apply();
    });
    $scope.countSum = function(numArray){
        var temp = 0;
        for (var i = 0; i< numArray.length; i++){
            temp = temp + Number(numArray[i]);
        }
        return temp;
    }; 
    $scope.uniqueValue = function(valueArray){
        return jQuery.unique(valueArray);
    };
    $scope.onClickEventQueriedData = function(currentCriteria, eventClickCriteria, eventFilteredCriteria, calledDataPayload, scope){
        var tempDataArray = [];
        var filteredQuery = jsonPath(calledDataPayload, "$.audits[?(@"+eventFilteredCriteria+" == '"+eventClickCriteria+"')]"+currentCriteria+"");
        console.log(filteredQuery);
        var uniqueResults = scope.uniqueValue(filteredQuery);
        var queryPerEventCriteria = "$.audits[?(@"+eventFilteredCriteria+" == '"+eventClickCriteria+"')]";
        var auditsPerEventCriteria = {};
        auditsPerEventCriteria["audits"] = jsonPath(calledDataPayload, queryPerEventCriteria);
        console.log(auditsPerEventCriteria);
        for (var i = 0; i < uniqueResults.length; i++){
            var queryCount = "$.audits[?(@"+currentCriteria+" == '"+uniqueResults[i]+"')].count";;
            var uniqueValueCount = jsonPath(auditsPerEventCriteria, queryCount);
            var sumOfUniqueValues = scope.countSum(uniqueValueCount);
            var pieEntry = {"_id":uniqueResults[i], "count":sumOfUniqueValues}; 
            tempDataArray.push(pieEntry);
        };
        return tempDataArray
    };
    $scope.onClickEventSecondTier = function(currentCriteria, firstEventClickedCriteria, firstEventFilteredCriteria,
                                             secondEventClickedCriteria, secondEventFilteredCriteria,
                                             calledDataPayload, scope){
            var tempDataArray = [];
            var queryPerEventCriteria = "$.audits[?(@"+firstEventFilteredCriteria+" == '"+firstEventClickedCriteria+"')]";
            var auditsPerEventCriteria = {};
            auditsPerEventCriteria["audits"] = jsonPath(calledDataPayload, queryPerEventCriteria);
            var secondTierFilteredQuery = jsonPath(auditsPerEventCriteria,"$.audits[?(@"+secondEventFilteredCriteria+" =='"
                    +secondEventClickedCriteria+"')]"+currentCriteria+"");
            var secondQueryPerEventCriteria = "$.audits[?(@"+secondEventFilteredCriteria+" == '"+secondEventClickedCriteria+"')]";
            secondTierAuditsPerEventCriteria = {};
            secondTierAuditsPerEventCriteria["audits"] = jsonPath(auditsPerEventCriteria, secondQueryPerEventCriteria);
            var uniqueResults = scope.uniqueValue(secondTierFilteredQuery);
            for (var i = 0; i < uniqueResults.length; i++){
                var queryCount = "$.audits[?(@"+currentCriteria+" == '"+uniqueResults[i]+"')].count";;
                var uniqueValueCount = jsonPath(secondTierAuditsPerEventCriteria, queryCount);
                var sumOfUniqueValues = scope.countSum(uniqueValueCount);
                var pieEntry = {"_id":uniqueResults[i], "count":sumOfUniqueValues}; 
                tempDataArray.push(pieEntry);
            }
            return tempDataArray;
    };
    $scope.pieQueryData = function(filterCriteria, calledDataPayload, scope){
        var tempDataArray = [];
        var filteredQuery = jsonPath(calledDataPayload, "$.audits[*]"+filterCriteria+"");
        var uniqueResults = scope.uniqueValue(filteredQuery);
        for (var i = 0; i < uniqueResults.length; i++){
            var queryCount = "$.audits[?(@"+filterCriteria+" =='"+uniqueResults[i]+"')].count";
            var uniqueValueCount = jsonPath(calledDataPayload, queryCount);
            var sumOfUniqueValues = scope.countSum(uniqueValueCount);
            var pieEntry = {"_id":uniqueResults[i], "count":sumOfUniqueValues}; 
            tempDataArray.push(pieEntry);
        };
        return tempDataArray
    };
    $scope.severityPieChartQueriedData = $scope.pieQueryData("._id.severity", $scope.calledData, $scope);
    $scope.appBarChartQueriedData = $scope.pieQueryData("._id.application", $scope.calledData, $scope);
    $scope.interfaceChartQueriedData = $scope.pieQueryData("._id.interface", $scope.calledData, $scope);
        
    $scope.onSeverityPieChart = function(onClickEvent){
        currentSeverity = onClickEvent.srcElement.__data__.data.label;
        $scope.appBarChartQueriedData = $scope.onClickEventQueriedData("._id.application", currentSeverity, "._id.severity", $scope.calledData, $scope);
        $scope.interfaceChartQueriedData = $scope.onClickEventQueriedData("._id.interface", currentSeverity, "._id.severity", $scope.calledData, $scope);
    };
    $scope.onAppPieChart = function(onClickEvent){
        currentApplication = onClickEvent.srcElement.__data__._id;
        if (currentSeverity == ""){
            $scope.interfaceChartQueriedData = $scope.onClickEventQueriedData("._id.interface", currentApplication, "._id.application", $scope.calledData, $scope);
        }
        else {
            $scope.interfaceChartQueriedData = $scope.onClickEventSecondTier("._id.interface", currentSeverity, "._id.severity", 
                                      currentApplication, "._id.application", $scope.calledData, $scope);
        }
        
    };
    $scope.replaceGraph = function(divID, newData, element, newGraph){
        var edit = document.getElementById(divID);
        edit.removeChild(edit.childNodes[0]);
        newGraph(newData,element);
    };
    //customService.prepForBroadcast();
    //var temp = customService.httpResponse;
    //var jsonData = {}
    /*temp.then(function(data){
        //console.log(data);
        jsonData['audtis'] = data.data._embedded['rh:doc'];
        $scope.calledData = jsonData;
        
        //console.log($scope.calledData)
        $scope.severityPieChartQueriedData = $scope.pieQueryData("._id.severity", jsonData, $scope);
    });*/
    $scope.$on('sliderChange', function(d){
        d.stopPropagation();
        console.log(d.targetScope.sliderValue);
        var currentTimestamp = Date.now();
        console.log(currentTimestamp);
    });
}]);

