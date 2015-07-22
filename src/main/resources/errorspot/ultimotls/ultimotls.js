/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//GLOBAL VARIABLES FOR INTITIAL SETUP
var TLS_PROTOCOL = "http";
var TLS_AUDIT_COLLECTION = "ErrorSpotActual";
var TLS_SETTING_COLLECTION = "ErrorSpotSetting";
var TLS_PAYLOAD_COLLECTION = "payloadCollection";
var TLS_SERVER = "172.16.120.157";
var TLS_PORT = "8080";
var TLS_DBNAME = "ES";
var TLS_SERVER_TIMEOUT = 3000;
var TLS_BATCH_REPLAY_COLLECTION = "ErrorSpotBatchReplay";
var TLS_EXPIRATION_TIME =  15 //in minutes
   
//(function(angular){
var ultimotls = angular.module('ultimotls', ['auditControllerModule', 'sunburstDirectiveModule', 'auditDirectiveModule' , 'treemapDirectiveModule', 'base64', 
                                             'LocalStorageModule', 'settingModule', 'ui.router', 'severityPieChartDirectiveModule', 'errorPieChartDirectiveModule',
                                             'transactionTypeBarChartDirectiveModule']);
ultimotls.controller('loginControllerModule', ['$scope', '$http', '$q', '$base64', '$location','localStorageService', 'treemapSaver','$timeout','queryEnv',
    function ($scope, $http, $q, $base64, $location, localStorageService, treemapSaver, $timeout, queryEnv ){ //loging Controller
        $scope.cred;
        $scope.treemapSaver = treemapSaver;
        $scope.treemapSaver.showNav = false;
        console.log('*************** LoginCtrl');
        $scope.treemapSaver.nameSaver=localStorageService.cookie.get('name')
        $scope.isLoggedin = function () {
            var _credentials = localStorageService.cookie.get('creds');
            
            if (angular.isUndefined(_credentials) || _credentials === null) {
                return false;
                
            }
            else {
                return true;
                
            }
        }
        $scope.login = function () {
            $scope.cred.screen = false;
            $scope.authError = false;
            $scope.authWrongCredentials = false;
            
            var credentials = $base64.encode($scope.cred.username + ":" + $scope.cred.password);
            $scope.treemapSaver.nameSaver = $scope.cred.username;
            console.log('*** authorization header: ' + credentials);

            $http.defaults.headers.common["Authorization"] = 'Basic ' + credentials;
            $http.defaults.headers.common["No-Auth-Challenge"];
            if(!$scope.cred.envid){
                $scope.cred.envid = "PROD";
            }
            $http.defaults.headers.common["Env-ID"] = $scope.cred.envid;
            //addd env in the header
            //promise to return
            var deferred = $q.defer();

            var request = $http.get(TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/LoginService/"+$scope.cred.username, {});
            request.success(function (data, status, header, config) {
                var auth_token = header()['auth-token'] //pulling our auth-token
                //creating credentials based off of username and auth-token
                credentials = $base64.encode($scope.cred.username + ":" + auth_token);
                localStorageService.cookie.add('name', $scope.cred.username, TLS_EXPIRATION_TIME/(24*60));
                if (!angular.isUndefined(data) && data !== null && !angular.isUndefined(data.authenticated) && data.authenticated) {
                    $scope.loginError = ""
                    console.log('*** authenticated.');
                    console.log('*** user roles: ' + data.roles);
                    console.log(credentials);
                    $scope.treemapSaver.showNav = true;
                    localStorageService.cookie.add('showNav', $scope.treemapSaver.showNav, TLS_EXPIRATION_TIME/(24*60));
                    $http.defaults.headers.common["Authorization"] = 'Basic ' + credentials;
                    localStorageService.cookie.add('creds', credentials, TLS_EXPIRATION_TIME/(24*60));
                    $timeout(function() {
                        delete $http.defaults.headers.common["Authorization"];
                        console.log('Authorization Expired')
                    }, TLS_EXPIRATION_TIME*60*1000);
                    $scope.$apply($location.path("/treemap"));
                    
                    
                    //Change location to Dashboard Page
                    //$location.path('/posts/');

                    deferred.resolve();
                }
                else {
                    $scope.loginError = "Username and/or password is incorrect"
                    console.log('*** authentication failed. wrong credentials.');
                    localStorageService.cookie.remove('creds');
                    delete $http.defaults.headers.common["Authorization"];
                    $scope.authWrongCredentials = true;

                    //reject promise
                    deferred.reject('authentication failed..');
                }
            });
//
            request.error(function (data, status, header, config) {
                $scope.loginError = "Username and/or password is incorrect"
                console.log('authentication error');
                console.log(status);
                console.log(data);
                console.log(header);
                console.log(config);

                localStorageService.remove('creds');
                delete $http.defaults.headers.common["Authorization"];
                $scope.authError = true;

                //reject promise
                deferred.reject('authentication failed..');
            });
        };

        $scope.logout = function () {
            $scope.$apply($location.path("/login"));
            console.log($http.defaults.headers.common["Authorization"])
            $scope.auth = false;
            localStorageService.cookie.remove('creds');
            localStorageService.cookie.remove('showNav');
            delete $http.defaults.headers.common["Authorization"];
            
            //UNTESTED DELETE FUNCTION
//            $http({method: 'DELETE',headers: {'Authorization': $http.defaults.headers.common["Authorization"]},
//                url: TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_authtokens/" +$scope.treemapSaver.nameSaver                
//            })
            //UNTESTED DELETE FUNCTION
            
            
        };
        $http.get(TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/"+TLS_DBNAME+"/"+TLS_SETTING_COLLECTION+"/SettingService?object=setting.envsetup",{timeout:TLS_SERVER_TIMEOUT})
            .success(function(data){
                var envOptionsData = data._embedded['rh:doc'][0].envsetup;
                $scope.envOptions = envOptionsData;
                $scope.envSelected = $scope.envOptions[0]; 
                $scope.env = queryEnv.getEnv();
                if(typeof $scope.treemapSaver.env !== 'undefined'){ //checks whether or not the env value holder in the service exists yet
                    for(var i =0; i< $scope.envOptions.length; i++){
                        if ($scope.treemapSaver.env.name === $scope.envOptions[i].label){
                            $scope.envSelected = $scope.envOptions[i]; 
                        }
                    };  
                };
            });
            
            
//        $scope.envOptions = [{name:"Prod", description: "Production", dbName:"PROD"}, 
//                         {name:"QA", description:"QA", dbName:"QA"}, 
//                         {name:"Dev", description: "Developement", dbName:"DEV"}];
        $scope.setEnvironment = function(env){//Set the environment when changed
            $scope.envSelected = env
            $scope.treemapSaver.env = env
            queryEnv.setEnv(env);
            queryEnv.broadcast();
        };
}]);

ultimotls.run(['$rootScope', '$location', 'treemapSaver', 'localStorageService', '$http', 
    function ($rootScope, $location, treemapSaver, localStorageService, $http) {
        
        $rootScope.$on('$stateChangeStart', function (event) {
            var _credentials = localStorageService.cookie.get('creds');
            treemapSaver.showNav = localStorageService.cookie.get('showNav');
            if (angular.isUndefined(_credentials) || _credentials === null) {
                console.log('NO CREDENTIALS');
                if(document.getElementById("loginContainter") !== null)event.preventDefault();
                delete $http.defaults.headers.common["Authorization"];
                $location.path('/login');
                return false;
                
            }
            else {
                if (!treemapSaver.showNav) {
                    console.log('ACCESS DENIED');
                    if(document.getElementById("loginContainter") !== null)event.preventDefault();
                    $location.path('/login');
                }
                else {
                    $http.defaults.headers.common["Authorization"] = 'Basic ' + _credentials;
                    console.log('ACCESS GRANTED');
                }
            }
            
    });
}]);


ultimotls.filter('unique', function () {

    return function (items, filterOn) {

        if (filterOn === false) {
            return items;
        }

        if ((filterOn || angular.isUndefined(filterOn)) && angular.isArray(items)) {
            var hashCheck = {}, newItems = [];

            var extractValueToCompare = function (item) {
                if (angular.isObject(item) && angular.isString(filterOn)) {
                    return item[filterOn];
                } else {
                    return item;
                }
            };

            angular.forEach(items, function (item) {
                var valueToCheck, isDuplicate = false;

                for (var i = 0; i < newItems.length; i++) {
                    if (angular.equals(extractValueToCompare(newItems[i]), extractValueToCompare(item))) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    newItems.push(item);
                }

            });
            items = newItems;
        }
        return items;
    };
});

ultimotls.controller('getTabs', ['$scope', '$location', function($scope, $location){
    $scope.tabBuilder = function(){
//        };
         $scope.tabs = [
            { link : '#/treemap', label : ' Dashboard' },
            { link : '#/audits', label : 'Audits' }
            //{ link : '#/sunburst', env:env.name, label : ' Sunburst Dashboard' }
          ]; 
        $scope.setTab = null;

        $scope.currentPath = $location.path();
        for(var tabCounter = 0; tabCounter < $scope.tabs.length; tabCounter++){
            if($scope.currentPath === $scope.tabs[tabCounter].link.substring(1)){
                $scope.setTab = tabCounter;
            }
        }
        $scope.selectedTab = $scope.tabs[$scope.setTab];    
        $scope.setSelectedTab = function(tab) {
          $scope.selectedTab = tab;
        };
        $scope.tabClass = function(tab) {
          if ($scope.selectedTab === tab) {
            return "active";
          } else {
            return "";
          } 

        };
    }
}]);

ultimotls.directive('tabsPanel', function () {
    return{
        restrict: 'E',
        scope: true,
        templateUrl: 'navTabs.html',
        controller: 'getTabs',
        link : function ($scope, $location) {
            $scope.tabBuilder();
            $scope.$on('$locationChangeStart', function(event) {
                $scope.tabBuilder();
            });
        }
    };
});

ultimotls.config(function ($stateProvider, $urlRouterProvider) {
    
    $urlRouterProvider.otherwise("/login");
    
    $stateProvider
        .state('sunburst',{
            url: "/sunburst",
            templateUrl: 'ultimotls/dashboard/sunburst/sunburstDashboard.html'
        })
        .state('audits', {
            url: "/audits",
            templateUrl: 'ultimotls/audit/searchApp.html',
            controller: 'DataRetrieve',
            resolve: {
                initPromise:['auditSearch','auditQuery', function(auditSearch, auditQuery){
                    var rowNumber = {'rows': 25};
                    var query = auditQuery.query();

                    if( query!= ''){
                        var data = auditSearch.doSearch(query, rowNumber, "audit");

                       return data;
                    }

                    return;
                }]
            }
        })
        .state('treemap', {
            url: "/treemap",
            templateUrl: 'ultimotls/dashboard/treemap/treemapDashboard.html'
        })
        .state('setting', {
            url: "/setting",
            templateUrl: 'ultimotls/setting/settings.html'
        })
        .state('login', {
            url:"/login",
            templateUrl: 'ultimotls/login.html',
            controller: 'loginControllerModule'
        })
    // this trick must be done so that we don't receive
    // `Uncaught Error: [$injector:cdep] Circular dependency found`

});

ultimotls.factory("mongoAggregateService", function ($http) {
    var postUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/"+TLS_DBNAME+"/"+TLS_AUDIT_COLLECTION+"/aggregate";
    var callAggregate = {};
    callAggregate.httpResponse = {};
    callAggregate.prepForBroadcast = function () {
        this.httpResponse = this.callHttp();
    };
    callAggregate.callHttp = function (payload) {
        var promise = $http.post(postUrl, payload, {timeout:TLS_SERVER_TIMEOUT}).success(function (result) {
            //console.log(result);
        }).error(function () { //need to pass error message through the service???
            console.log("error");
        });
        return promise;
    };
    return callAggregate;
});

ultimotls.service("queryEnv", function($rootScope){ //getter and setter for environment 
    var envid = {}
    envid.label = "Prod", envid.name = "PROD"
    var environment = {};
    
    environment.setEnv = function(env){
        if(env){
            envid.label = env.label;
            envid.name = env.name;
        }
        return envid;
    };
    environment.getEnv = function(){ //remove later
        return envid;
    };
    environment.broadcast = function(){
        $rootScope.$broadcast("envChangeBroadcast")
    }
    return environment;
});
ultimotls.service("timeService", function($rootScope){ //getter and setter for drop down value 
    var currentDateTime = new Date();
    var timeSelected = {};
    timeSelected.toDate = new Date(currentDateTime).toISOString(); 
    timeSelected.fromDate = new Date(currentDateTime - (1*60*60*1000)).toISOString(); //default one hour if no setTime 
    timeSelected.value = 1 //default 1 hour
    var time = {};
    
    time.setTime = function(fromDate, toDate){
        if(time){
            timeSelected.toDate = toDate
            timeSelected.fromDate = fromDate
        }
        return timeSelected;
    };
    time.getTime = function(){ //remove later
        return timeSelected;
    };
    time.broadcast = function(){
        $rootScope.$broadcast("timeChangeBroadcast")
    }
    return time;
});

ultimotls.service("queryFilter", function($rootScope){
    var filter = {};
    var newFilter = ""
    filter.appendQuery = function(name,value){
        if(name && value){
            newFilter = "\""+name+"\":\""+value+"\",";
        }
        return newFilter;
    };
    filter.broadcast = function(){
        $rootScope.$broadcast("newFilterAppended")
    };
    return filter;
});

ultimotls.service("auditSearch",['$http','queryEnv', function ($http, queryEnv) {
    var postUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/SearchService?filter=";
    var audits = {};
    var env = queryEnv.getEnv();
    audits.doSearch = function (searchCriteria, rowNumber, dbType) {
//    textSearch and jsonSearch WILL BE SWAPPED WITH THESE
    var textSearch = "{\"$and\":[{\"envid\":\""+env.name+"\"},{$text:{$search:'" + searchCriteria + "'}}]}&searchtype=basic&searchdb="+dbType+"&count&pagesize=" + rowNumber.rows;
    var jsonSearch = "{\"$and\":[{\"envid\":\""+env.name+"\"},"+searchCriteria + "]}&searchtype=basic&searchdb="+dbType+"&count&pagesize=" + rowNumber.rows;
        var searchPromise = {};
        if (/:/.test(searchCriteria)) {
                var jsonUrl = postUrl + jsonSearch;
                searchPromise = $http.get(jsonUrl, {timeout:TLS_SERVER_TIMEOUT}).success(function (response) {

                }).error(function () {
                    console.log("error");
                });
                audits.inputError = "";
        }
        else {
            var textUrl = postUrl + textSearch;
            searchPromise = $http.get(textUrl, {timeout:TLS_SERVER_TIMEOUT}).success(function(response){
                
            }).error(function () {
                console.log("error");
            });
            audits.inputError = "";
        }

        return searchPromise;
    };

    return audits;
}]);


//common service to get query string from other sunburst controllers
ultimotls.service("auditQuery", function () {
    var queryParam  = "";

    
    return {
        query: function(param){
            
            if (param)
            {
                queryParam =  param;
                
            }
        
            return queryParam;
        }
    }
    
});

ultimotls.factory("treemapSaver", function() {
    var treemapSaver = {};
    treemapSaver.dropdownVal = 1;
    return treemapSaver;
});
ultimotls.factory("sunburstSaver", function() {
    var sunburstSaver = {};
    sunburstSaver.dropdownVal = 1;
    return sunburstSaver;
});

//})(window.angular);
