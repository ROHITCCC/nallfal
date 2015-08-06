/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//GLOBAL VARIABLES FOR INTITIAL SETUP
//var TLS_PROTOCOL = "http";
//var TLS_SERVER = "172.16.120.157";
//var TLS_PORT = "8080";
//var TLS_DBNAME = "ES";
//var TLS_SERVER_TIMEOUT = 6000;
var TLS_PROTOCOL = location.protocol.replace(/[:]/g , '');
var TLS_SERVER = location.hostname;
var TLS_PORT = location.port;
var TLS_SERVER_TIMEOUT = 6000;
   
//(function(angular){
var ultimotls = angular.module('ultimotls', ['auditControllerModule', 'auditDirectiveModule' , 'treemapDirectiveModule', 'base64', 
                                             'LocalStorageModule', 'settingModule', 'ui.router', 'severityPieChartDirectiveModule', 'errorPieChartDirectiveModule',
                                             'transactionTypeBarChartDirectiveModule']);
ultimotls.controller('loginControllerModule', ['$scope', '$http', '$q', '$base64', '$location','localStorageService', 'treemapSaver','queryEnv','resetTimerService',
    function ($scope, $http, $q, $base64, $location, localStorageService, treemapSaver, queryEnv, resetTimerService ){ //loging Controller
        $scope.cred;
        $scope.treemapSaver = treemapSaver;
        $http.defaults.headers.common["No-Auth-Challenge"];
        if(treemapSaver.envError){
            $scope.loginError = treemapSaver.envError;
            treemapSaver.envError = "";
        }
        if(localStorageService.cookie.get('showNav')){//Will check to see if user is still logged in
            var username = localStorageService.cookie.get('name');
            var cred = localStorageService.cookie.get('creds'); 
            var envID =  localStorageService.cookie.get('envid');
            
            $http.defaults.headers.common["Authorization"] = 'Basic ' + cred;
            $http.defaults.headers.common["No-Auth-Challenge"];
            $http.defaults.headers.common["Env-ID"] = envID;
            
            var deferred = $q.defer();
            
            var loggedInRequest = $http.get(TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/LoginService/"+username, {timeout:TLS_SERVER_TIMEOUT});
            loggedInRequest.success(function (data, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                treemapSaver.username = username;
                if (!angular.isUndefined(data) && data !== null && !angular.isUndefined(data.authenticated) && data.authenticated) {
                    $scope.loginError = "";
                    resetTimerService.set(auth_token_valid_until);
                    treemapSaver.envid = envID
                    var roles = data.roles;
                    for(var i =0;i<roles.length;i++){
                        if(roles[i] === "admins"){
                            treemapSaver.environmentBiPass = true;
                            break;
                        }
                    };
                    queryEnv.broadcastLogin();
                    $scope.$apply($location.path("/treemap"));
                    deferred.resolve();
                }
                else {
                    $scope.loginError = "Username and/or password is incorrect";
                    localStorageService.cookie.remove('creds');
                    delete $http.defaults.headers.common["Authorization"];
                    //reject promise
                    deferred.reject('authentication failed..');
                }
            });
        };
        clearError = function(){
            $scope.loginError = "";
        };
        $scope.login = function () {
            $scope.treemapSaver.credEnvId = $scope.cred.envid;
            var credentials = $base64.encode($scope.cred.username + ":" + $scope.cred.password);
            $scope.treemapSaver.nameSaver = $scope.cred.username;

            $http.defaults.headers.common["Authorization"] = 'Basic ' + credentials;
            $http.defaults.headers.common["No-Auth-Challenge"] = "true";
            if(!$scope.cred.envid){
                $scope.cred.envid = "PROD";
            };
            $http.defaults.headers.common["Env-ID"] = $scope.cred.envid;
            var deferred = $q.defer();
            var request = $http.get(TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/LoginService/"+$scope.cred.username, {timeout:TLS_SERVER_TIMEOUT});
            
            request.success(function (data, status, header, config) {
                var auth_token = header()['auth-token'];
                var auth_token_valid_until = header()['auth-token-valid-until'];
                credentials = $base64.encode($scope.cred.username + ":" + auth_token);
                treemapSaver.username = $scope.cred.username;
                localStorageService.cookie.add('name', $scope.cred.username);
                if (!angular.isUndefined(data) && data !== null && !angular.isUndefined(data.authenticated) && data.authenticated) {
                    $scope.loginError = "";
                    treemapSaver.showNav = true;
                    localStorageService.cookie.add('showNav', treemapSaver.showNav);
                    $http.defaults.headers.common["Authorization"] = 'Basic ' + credentials;
                    localStorageService.cookie.add('creds', credentials);
                    resetTimerService.set(auth_token_valid_until);
                    treemapSaver.envid = $scope.cred.envid;
                    treemapSaver.environmentBiPass = false;
                    var roles = data.roles;
                    for(var i =0;i<roles.length;i++){
                        if(roles[i] === "admins"){
                            treemapSaver.environmentBiPass = true;
                            break;
                        }
                    };
                    queryEnv.broadcastLogin();
                    //$scope.$apply($location.path("/treemap"));
                    $location.path("/treemap");
                    deferred.resolve();
                }
                else {
                    $scope.loginError = "Username and/or password is incorrect";
                    localStorageService.cookie.remove('creds');
                    delete $http.defaults.headers.common["Authorization"];
                    //reject promise
                    deferred.reject('authentication failed..');
                }
            });
            request.error(function (data, status, header, config) {
                if(status === 0){
                    $scope.loginError = "Backend timed out";
                }
                if(status === 401){
                    $scope.loginError = "Username and/or password is incorrect";
                }
                localStorageService.cookie.remove('creds');
                localStorageService.cookie.remove('showNav');
                localStorageService.cookie.remove('name');
                localStorageService.cookie.remove('envOptions');
                localStorageService.cookie.remove('envid');
                delete $http.defaults.headers.common["Authorization"];
                //reject promise
                deferred.reject('authentication failed..');
            });
        };
}]);
ultimotls.service("logoutService", ['$http','$location','localStorageService','treemapSaver',
    function($http, $location, localStorageService, treemapSaver){
        var logoutService = {};
        logoutService.logout = function(){
            localStorageService.cookie.remove('creds');
            localStorageService.cookie.remove('showNav');
            localStorageService.cookie.remove('name');
            localStorageService.cookie.remove('envOptions');
            localStorageService.cookie.remove('envid');
            delete $http.defaults.headers.common["Authorization"];
            treemapSaver.showNav = false;
            $location.path("/login");
        }
        return logoutService;
}]);
ultimotls.controller("indexControllerModule", ['$scope','localStorageService','treemapSaver','queryEnv','logoutService',
    function($scope,localStorageService,treemapSaver,queryEnv,logoutService){
        $scope.treemapSaver = treemapSaver;
        $scope.treemapSaver.showNav = localStorageService.cookie.get('showNav');
        $scope.treemapSaver.nameSaver = localStorageService.cookie.get('name');
//        $scope.logout = function () {
//            $scope.auth = false;
//            localStorageService.cookie.remove('creds');
//            localStorageService.cookie.remove('showNav');
//            localStorageService.cookie.remove('name');
//            localStorageService.cookie.remove('envOptions');
//            localStorageService.cookie.remove('envid');
//            delete $http.defaults.headers.common["Authorization"];
//            $scope.treemapSaver.showNav = false;
//            $scope.$apply($location.path("/login"));
//        };
        $scope.logout = function(){
            logoutService.logout();
        };
        $scope.setEnvironment = function(env){//Set the environment when changed
            if(!env){
                return;
            };
            localStorageService.cookie.add('envid', env);
            $scope.envSelected = env;
            $scope.treemapSaver.env = env;
            queryEnv.setEnv(env);
            queryEnv.broadcast();
        };
        $scope.$on("performedLogin", function(){
            $scope.treemapSaver.showNav = true;
            queryEnv.getEnvOptions().then(function(response){
                $scope.envOptions = response.data._embedded['rh:doc'][0].envsetup;
                var environmentFound = treemapSaver.environmentBiPass;
                for(var i =0; i< $scope.envOptions.length; i++){
                    if (treemapSaver.envid === $scope.envOptions[i].name){
                        $scope.envSelected = $scope.envOptions[i]; 
                        $scope.setEnvironment($scope.envSelected);
                        environmentFound = true;
                        break;
                    }
                };
                if(!environmentFound){
                    treemapSaver.envError = "Environment has not been setup";
                    $scope.logout();
                };
                localStorageService.cookie.add('envOptions',$scope.envOptions);
                localStorageService.cookie.add('name', treemapSaver.username);
            });
        });
        $scope.envSelected = localStorageService.cookie.get('envid');
        $scope.setEnvironment($scope.envSelected);
        $scope.envOptions = localStorageService.cookie.get('envOptions');
}]);
ultimotls.run(['$rootScope', '$location', 'treemapSaver', 'localStorageService', '$http','logoutService', 
    function ($rootScope, $location, treemapSaver, localStorageService, $http, logoutService) {
        $http.defaults.headers.common["No-Auth-Challenge"] = "true";
        $rootScope.$on('$stateChangeStart', function (event) {
            var _credentials = localStorageService.cookie.get('creds');
            treemapSaver.showNav = localStorageService.cookie.get('showNav');
            if (angular.isUndefined(_credentials) || _credentials === null) {
                if(document.getElementById("loginContainter") !== null)event.preventDefault();
                logoutService.logout();
                return false;
            }
            else {
                if (!treemapSaver.showNav) {
                    if(document.getElementById("loginContainter") !== null)event.preventDefault();
                    logoutService.logout();
                }
                else {
                    $http.defaults.headers.common["Authorization"] = 'Basic ' + _credentials;
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
         $scope.tabs = [
            { link : '#/treemap', label : ' Dashboard' },
            { link : '#/audits', label : 'Audits' }
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
    };
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
        .state('login', {
            url:"/login",
            templateUrl: 'ultimotls/login.html',
            controller: 'loginControllerModule'
        })
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

                    if( query!== ''){
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
        });
    // this trick must be done so that we don't receive
    // `Uncaught Error: [$injector:cdep] Circular dependency found`
});
ultimotls.factory("mongoAggregateService", ['$http','resetTimerService','logoutService',
    function ($http,resetTimerService,logoutService) {
    var postUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/AggregateService";
    var callAggregate = {};
    callAggregate.httpResponse = {};
    callAggregate.prepForBroadcast = function () {
        this.httpResponse = this.callHttp();
    };
    callAggregate.callHttp = function (payload) {
        var promise = $http.post(postUrl, payload, {timeout:TLS_SERVER_TIMEOUT})
            .success(function (result, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
        })
        .error(function (result, status, header, config) { //need to pass error message through the service???
            if(status===401){
                logoutService.logout();
            }
        });
        return promise;
    };
    return callAggregate;
}]);
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
ultimotls.service("queryEnv",['$http', '$rootScope', function($http,$rootScope){ //getter and setter for environment 
    var envid = {};
    envid.label = "Prod", envid.name = "PROD";
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
    environment.getEnvOptions = function(){
        var promise = $http.get(TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/SettingService?object=setting.envsetup",{timeout:TLS_SERVER_TIMEOUT})
            .success(function(data){
            })
            .error(function(data, status, header, config){
                return null;
            });
        return promise;
    };
    environment.broadcast = function(){
        $rootScope.$broadcast("envChangeBroadcast");
    };
    environment.broadcastLogin = function(){
        $rootScope.$broadcast("performedLogin");
    };
    return environment;
}]);
ultimotls.service("timeService", function($rootScope){ //getter and setter for drop down value 
    var currentDateTime = new Date();
    var timeSelected = {};
    timeSelected.toDate = new Date(currentDateTime).toISOString(); 
    timeSelected.fromDate = new Date(currentDateTime - (1*60*60*1000)).toISOString(); //default one hour if no setTime 
    timeSelected.value = 1; //default 1 hour
    var time = {};
    time.setTime = function(fromDate, toDate){
        if(time){
            timeSelected.toDate = toDate;
            timeSelected.fromDate = fromDate;
        }
        return timeSelected;
    };
    time.getTime = function(){ //remove later
        return timeSelected;
    };
    time.broadcast = function(){
        $rootScope.$broadcast("timeChangeBroadcast");
    };
    return time;
});
ultimotls.service("queryFilter", function($rootScope){
    var filter = {};
    var newFilter = "";
    filter.appendQuery = function(name,value){
        if(name && value){
            newFilter = "\""+name+"\":\""+value+"\",";
        }
        if(name === "" && value === ""){
            newFilter = "";
        }
        return newFilter;
    };
    filter.broadcast = function(){
        $rootScope.$broadcast("newFilterAppended");
    };
    return filter;
});
ultimotls.service("auditSearch",['$http','queryEnv', 'resetTimerService','logoutService',function ($http, queryEnv,resetTimerService,logoutService) {
    var postUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/SearchService?filter=";
    var audits = {};
    var env = queryEnv.getEnv();
    audits.doSearch = function (searchCriteria, rowNumber, dbType) {
        var textSearch = "{\"$and\":[{\"envid\":\""+env.name+"\"},{$text:{$search:'"+ searchCriteria+"'}}]}&searchtype=basic&searchdb="+dbType+"&count&pagesize=" + rowNumber.rows;
        var jsonSearch = "{\"$and\":[{\"envid\":\""+env.name+"\"},"+searchCriteria +"]}&searchtype=basic&searchdb="+dbType+"&count&pagesize=" + rowNumber.rows;
        var searchPromise = {};
        if (/:/.test(searchCriteria)) {
            var jsonUrl = postUrl + jsonSearch;
            searchPromise = $http.get(jsonUrl, {timeout:TLS_SERVER_TIMEOUT})
                .success(function (response, status, header, config) {
                    var auth_token_valid_until = header()['auth-token-valid-until'];
                    resetTimerService.set(auth_token_valid_until);
                    audits.inputError = "";
                })
                .error(function (response, status, header, config) {
                    if(status===0){
                        audits.inputError = "Backend timed out";
                    }
                    if(status===401){
                        audits.inputError = "Unauthorized";
                        logoutService.logout();
                    }
                });
            
        }
        else {
            var textUrl = postUrl + textSearch;
            searchPromise = $http.get(textUrl, {timeout:TLS_SERVER_TIMEOUT})
                .success(function(response, status, header, config){
                    var auth_token_valid_until = header()['auth-token-valid-until'];
                    resetTimerService.set(auth_token_valid_until);
                    audits.inputError = "";
                })
                .error(function (response, status, header, config) {
                    if(status === 0){
                        audits.inputError = "Backend timed out";
                    }
                    if(status===401){
                        audits.inputError = "Unauthorized";
                        logoutService.logout();
                    }
                });
            
        }
        return searchPromise;
    };
    return audits;
}]);
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
    };
});
ultimotls.service("resetTimerService",['localStorageService', function(localStorageService){
    var resetTimer = {};
    resetTimer.set = function(newTime){
        var currentDate = new Date();
        var newDate = new Date(newTime);
        var newExpiration = ((newDate.getTime() - currentDate.getTime())/60000)/(24*60);
        var userCred = localStorageService.cookie.get('creds'),
            username = localStorageService.cookie.get('name'),
            showNav = localStorageService.cookie.get('showNav'),
            envOptions = localStorageService.cookie.get('envOptions'),
            envID = localStorageService.cookie.get('envid');
        localStorageService.cookie.add('creds', userCred, newExpiration);
        localStorageService.cookie.add('name', username, newExpiration);
        localStorageService.cookie.add('showNav', showNav, newExpiration);
        localStorageService.cookie.add('envOptions',envOptions, newExpiration);
        localStorageService.cookie.add('envid',envID,newExpiration);
    };
    return resetTimer;
}]);
//})(window.angular);
