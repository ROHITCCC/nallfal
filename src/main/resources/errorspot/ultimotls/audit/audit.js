/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/////JQUERY HANDLERS////
//Advance Search
$("#advanceSearch").click(function () {
    $("#freeFormSearch").hide("slow", function () {
        $("#advSearchBox").slideDown("fast");
    });
    $("#rowOptions").attr("ng-click", "doAdvanceSearch()");
});
$("#closeAdvanceField").click(function () {
    $("#advSearchBox").hide("slow", function () {
        $("#freeFormSearch").slideDown("fast");
    });
    $("#rowOptions").attr("ng-click", "doSearch()");
});

//Slide Bar
$("#openSlide").click(function () {
    $("#sliderContent").animate(
            {"right": "0"},
    "slow");
});
$(".btn-primary").click(function () {
    $("#sliderContent").animate(
            {"right": "-50%"},
    "slow");
});
$("html").dblclick(function () {
    $("#sliderContent").animate(
            {"right": "-50%"},
    "slow");
});

$('body').on('click', '.mainmodal .close', function () {
    $('#replayPage').modal('hide');
    $('#payloadPage').modal('hide');
    $('#transactionPage').modal('hide');
}).on('click', '.mainmodal .modal-footer .btn', function () {
    $('#replayPage').modal('hide');
    $('#payloadPage').modal('hide');
    $('#transactionPage').modal('hide');
});

(function ($) {
    $.fn.waitUntilExists = function (handler, shouldRunHandlerOnce, isChild) {
        var found = 'found';
        var $this = $(this.selector);
        var $elements = $this.not(function () {
            return $(this).data(found);
        }).each(handler).data(found, true);
        if (!isChild) {
            (window.waitUntilExists_Intervals = window.waitUntilExists_Intervals || {})[this.selector] = window.setInterval(function () {
                $this.waitUntilExists(handler, shouldRunHandlerOnce, true);
            }, 500);
        } else if (shouldRunHandlerOnce && $elements.length) {
            window.clearInterval(window.waitUntilExists_Intervals[this.selector]);
        }
        return $this;
    };
}(jQuery));

$(".nav li.titlemenu").waitUntilExists(function () {
    $(this).on({
        mouseenter: function () {
            $(this).children('div.menuselector').addClass('open');
        },
        mouseleave: function () {
            $(this).children('div.menuselector').removeClass('open');
        }
    });
});


$('tr.immidateinfo').waitUntilExists(function () {
    $(this).on({
        mouseenter: function () {
            $('input', this).removeClass('inputable');
            $('select', this).removeClass('inputable');
            $('td:last', this).prev().prev().prev('td').css('white-space', 'normal').removeClass('hideinterfacebtn');
        },
        mouseleave: function () {
            $('input', this).addClass('inputable');
            $('select', this).addClass('inputable');
            $('td:last', this).prev().prev().prev('td').css('white-space', 'nowrap').addClass('hideinterfacebtn');
        }
    });
});

$('.datetimepicker').waitUntilExists(function () {
    $('.datetimepicker').datetimepicker({
        dayOfWeekStart: 1,
        lang: 'en',
        startDate: '2015/01/05'
    });
    $(this).datetimepicker({step: 10, inline: false});
    $(this).datetimepicker('show');
    $(this).datetimepicker('hide');
});

$('.datetimepicker2').waitUntilExists(function () {
    $('.datetimepicker2').datetimepicker({
        timepicker:false,
        format:'m/d/Y'
    });
    $(this).datetimepicker('show');
     $(this).datetimepicker('hide');
});

$('#transactionTypeDiv').waitUntilExists(function () {
    $.fn.resizepiechart = function(){
        this.each(function(){
            var temporalsize = $(this).attr('width');
            $(this).css('width',temporalsize);
        });
    };
    $('#transactionTypeDiv').resizepiechart();
    $( window ).resize(function() {
        $('#transactionTypeDiv').resizepiechart();
    });
});

$('#customDateTimes').waitUntilExists(function () {
        if ($('#customDateTimes').text() === ""){
                $('#currentTimeSelect').hover(
                    function () {
                        $('#customDateTimes').css('display','block');
                    },
                    function () {
                        $('#customDateTimes').css('display','none');
                    }
                );  
        };
});