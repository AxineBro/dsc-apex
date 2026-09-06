function number_format(number, decimals, dec_point, thousands_sep)
{
    var i, j, kw, kd, km;

    if (isNaN(decimals = Math.abs(decimals)))
        decimals = 2;

    if (dec_point == undefined)
        dec_point = ",";

    if (thousands_sep == undefined)
        thousands_sep = ".";

    i = parseInt(number = (+number || 0).toFixed(decimals)) + "";

    if ((j = i.length) > 3)
        j = j % 3;
    else
        j = 0;

    km = (j ? i.substr(0, j) + thousands_sep : "");
    kw = i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + thousands_sep);
    kd = (decimals ? dec_point + Math.abs(number - i).toFixed(decimals).replace(/-/, 0).slice(2) : "");

    return km + kw + kd;
}

jQuery(function ($) {

    // Navbar menu
    var $navbarMenu = $('.navbar__menu');
    $('.navbar__menu-close').on('click', function () {
        $navbarMenu.removeClass('active');
        $('html').removeClass('html-lock');
        setTimeout(
                function () {
                    $navbarMenu.css({display: 'none'});
                },
                300
                );
    });
    $('.navbar__toggler').on('click', function () {
        $('html').addClass('html-lock');
        $navbarMenu.css({display: 'flex'});
        setTimeout(
                function () {
                    $navbarMenu.addClass('active');
                    scroll.onscreenresize();
                },
                300
                );
    });

    var scroll = $('.navbar__menu-mobile').niceScroll({
        cursorwidth: '3px',
        cursorborder: '',
        cursorcolor: '#000',
        cursorborderradius: 0,
        cursoropacitymax: .2,
        horizrailenabled: false,
        overflowx: false,
        nativeparentscrolling: false
    });

    $('.navbar__menu > .list-unstyled > li > .list-unstyled')
            .on('shown.bs.collapse', function () {
                scroll.onscreenresize();
            })
            .on('hidden.bs.collapse', function () {
                scroll.onscreenresize();
            });
    $('.fancybox').fancybox();
    $('.link-popup').fancybox({
        baseClass: 'popup',
        modal: true,
        animationEffect: 'fade',
        backFocus: false,
        trapFocus: false,
        type: 'ajax',
        beforeShow: function () {
            $navbarMenu.removeClass('active');
            $('html').removeClass('html-lock');
            $navbarMenu.css({display: 'none'});
        },
        beforeClose: function () {
            $('.form-control, .custom-control-input').tooltip('dispose');
        }
    });

    $('[data-fancybox^="photo"]').fancybox({
        /*baseTpl:
         '<div class="fancybox-container" role="dialog" tabindex="-1">' +
         '<div class="fancybox-bg"></div>' +
         '<div class="fancybox-inner">' +
         '<div class="fancybox-infobar"><span data-fancybox-index></span>&nbsp;/&nbsp;<span data-fancybox-count></span></div>' +
         '<div class="fancybox-toolbar">{{buttons}}</div>' +
         '<div class="fancybox-navigation">{{arrows}}</div>' +
         '<div class="fancybox-stage"></div>' +
         '</div>' +
         '</div>',
         afterLoad:function(fb, item){
         item.$content.remove('.fb-caption').append('<div class="fb-caption">' + $(item.$thumb.context).data('caption') + '</div>');
         },*/
        infobar: false,
        animationEffect: 'fade',
        backFocus: false,
        trapFocus: false,
        buttons: [
            'close'
        ],
        loop: true,
        lang: 'ru'
    });

    /*var $carousel = $('.carousel-home');
     if($carousel.length > 0)
     {
     if($carousel.find('.carousel-indicators li').length > 1)
     {
     var from = 20, to = 8, width = from, step = parseInt((from-to)/($carousel.find('li').length-1));
     $carousel.find('.carousel-indicators li').each(function() {
     $(this).css({'width':width, 'height':width});
     width -= step;
     });
     $carousel.on('slide.bs.carousel', function () {
     $carousel.find('.carousel-indicators li').each(function() {
     $(this).css({'width':width, 'height':width});
     width -= step;
     });
     });
     }
     }*/

    var bHide = true;
    $('.navbar__menu-desktop').superfish({
        popUpSelector: '.navbar__menu-sub',
        hoverClass: 'hover',
        speed: 0,
        speedOut: 0,
        delay: 800,
        onBeforeShow: function () {
            if (bHide)
            {
                var $this = $(this);
                $this.addClass('hover');
                bHide = false;
            }
        },
        onShow: function () {
            bHide = false;
        },
        onHide: function () {
            bHide = true;
        }
    });

    $('.achievements-item__number').each(function () {
        var $this = $(this),
                limit = parseInt($this.data('limit')),
                duration = 2000,
                startTime = null,
                startValue = 0;

        if (limit > 0) {
            $this.text(0);
            function animateCounter(timestamp) {
                if (!startTime)
                    startTime = timestamp;
                var elapsed = timestamp - startTime;
                var progress = Math.min(elapsed / duration, 1);

                var currentValue = Math.floor(progress * limit);
                $this.text(currentValue);

                if (progress < 1) {
                    requestAnimationFrame(animateCounter);
                } else {
                    $this.text(limit);
                }
            }
            requestAnimationFrame(animateCounter);
        }
    });

    $('.v-btn2').click(function (e) {
        e.preventDefault()
        $('.virtual-tur2').addClass('active')
        $('.virtual-tur2').prepend('<div class="v-close"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"	 viewBox="0 0 492 492" style="enable-background:new 0 0 492 492;" xml:space="preserve"><g><g><path d="M300.188,246L484.14,62.04c5.06-5.064,7.852-11.82,7.86-19.024c0-7.208-2.792-13.972-7.86-19.028L468.02,7.872 c-5.068-5.076-11.824-7.856-19.036-7.856c-7.2,0-13.956,2.78-19.024,7.856L246.008,191.82L62.048,7.872	c-5.06-5.076-11.82-7.856-19.028-7.856c-7.2,0-13.96,2.78-19.02,7.856L7.872,23.988c-10.496,10.496-10.496,27.568,0,38.052	L191.828,246L7.872,429.952c-5.064,5.072-7.852,11.828-7.852,19.032c0,7.204,2.788,13.96,7.852,19.028l16.124,16.116	c5.06,5.072,11.824,7.856,19.02,7.856c7.208,0,13.968-2.784,19.028-7.856l183.96-183.952l183.952,183.952	c5.068,5.072,11.824,7.856,19.024,7.856h0.008c7.204,0,13.96-2.784,19.028-7.856l16.12-16.116	c5.06-5.064,7.852-11.824,7.852-19.028c0-7.204-2.792-13.96-7.852-19.028L300.188,246z"/></g></g></svg></div>')
        $('.v-close').click(function () {
            $('.virtual-tur2').removeClass('active');
        });
    });

    /*var flatsTimerId;
     $('.form-flats').find('input[type="text"]').on('change keyup', function() {
     var $this = $(this);
     clearTimeout(flatsTimerId);
     flatsTimerId = setTimeout(function() {
     var v = $this.val(),
     v = v.replace(/[^.\d]+/g, '').replace(/^([^\.]*\.)|\./g, '$1');
     v = parseInt(v);
     if(isNaN(v))
     $this.val(0);
     else
     $this.val(number_format(v, 0, '.', ' '));
     }, 500);
     });*/

    /*var hash = document.location.hash;
     if(hash)
     {
     $('.nav-tabs a[href="'+hash+'"]').tab('show');
     }
     $('.navbar__top-menu a, .navbar__menu-desktop a').on('click', function() {
     var hash = $(this).attr('href').split('#')[1];
     if(hash)
     {
     $('.nav-tabs a[href="#'+hash+'"]').tab('show');
     }
     });*/

    var pagePath = '/otdelka';
    if (document.location.href.indexOf(pagePath) != -1)
    {
        function readDeviceOrientation()
        {
            // window.innerHeight is not supported by IE
            var winH = window.innerHeight ? window.innerHeight : jQuery(window).height();
            var winW = window.innerWidth ? window.innerWidth : jQuery(window).width();
            //force height for iframe usage
            if (!winH || winH == 0)
            {
                winH = '100%';
            }
            // set the height of the document
            jQuery('html').css('height', winH);
            // scroll to top
            window.scrollTo(0, 0);
        }

        jQuery(document).ready(function () {
            if (/(iphone|ipod|ipad|android|iemobile|webos|fennec|blackberry|kindle|series60|playbook|opera\smini|opera\smobi|opera\stablet|symbianos|palmsource|palmos|blazer|windows\sce|windows\sphone|wp7|bolt|doris|dorothy|gobrowser|iris|maemo|minimo|netfront|semc-browser|skyfire|teashark|teleca|uzardweb|avantgo|docomo|kddi|ddipocket|polaris|eudoraweb|opwv|plink|plucker|pie|xiino|benq|playbook|bb|cricket|dell|bb10|nintendo|up.browser|playstation|tear|mib|obigo|midp|mobile|tablet)/.test(navigator.userAgent.toLowerCase()))
            {
                if (/iphone/.test(navigator.userAgent.toLowerCase()) && window.self === window.top)
                {
                    jQuery('body').css('height', '100.18%');
                }
                // add event listener on resize event (for orientation change)
                if (window.addEventListener)
                {
                    window.addEventListener("load", readDeviceOrientation);
                    window.addEventListener("resize", readDeviceOrientation);
                    window.addEventListener("orientationchange", readDeviceOrientation);
                }
                //initial execution
                setTimeout(function () {
                    readDeviceOrientation();
                }, 10);
            }
        });

        function accessWebVr(curScene)
        {
            unloadPlayer();
            setTimeout(function () {
                loadPlayer(true, curScene);
            }, 100);
        }

        function accessStdVr(curScene)
        {
            unloadPlayer();
            setTimeout(function () {
                loadPlayer(false, curScene);
            }, 100);
        }

        function loadPlayer(isWebVr, curScene)
        {
            if (isWebVr)
            {
                embedpano({
                    id: "krpanoSWFObject",
                    xml: "/indexdata/index_vr.xml",
                    target: "panoDIV",
                    passQueryParameters: true,
                    bgcolor: "#000000",
                    html5: "only+webgl",
                    focus: false,
                    vars: {skipintro: true, norotation: true, startscene: curScene}
                });
            } else
            {
                var isBot = /bot|googlebot|crawler|spider|robot|crawling/i.test(navigator.userAgent);
                embedpano({
                    id: "krpanoSWFObject",
                    swf: "/indexdata/index.swf",
                    target: "panoDIV",
                    passQueryParameters: true,
                    bgcolor: "#000000",
                    focus: false,
                    html5: isBot ? "always" : "prefer",
                    vars: {startscene: curScene},
                    localfallback: "flash"
                });
            }
            //apply focus on the visit if not embedded into an iframe
            if (top.location === self.location)
            {
                kpanotour.Focus.applyFocus();
            }
        }

        function unloadPlayer()
        {
            if (jQuery('#krpanoSWFObject'))
            {
                removepano('krpanoSWFObject');
            }
        }

        function isVRModeRequested()
        {
            var querystr = window.location.search.substring(1);
            var params = querystr.split('&');
            for (var i = 0; i < params.length; i++)
            {
                if (params[i].toLowerCase() == "vr")
                {
                    return true;
                }
            }
            return false;
        }

        function vStart()
        {
            if (isVRModeRequested())
            {
                accessWebVr();
            } else
            {
                accessStdVr();
            }
        }
        vStart();

        $('.v-btn').click(function (e) {
            e.preventDefault();
            $('.virtual-tur').addClass('active');
            $('.virtual-tur').prepend('<div class="v-close"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"	viewBox="0 0 492 492" style="enable-background:new 0 0 492 492;" xml:space="preserve"><g><g><path d="M300.188,246L484.14,62.04c5.06-5.064,7.852-11.82,7.86-19.024c0-7.208-2.792-13.972-7.86-19.028L468.02,7.872 c-5.068-5.076-11.824-7.856-19.036-7.856c-7.2,0-13.956,2.78-19.024,7.856L246.008,191.82L62.048,7.872	c-5.06-5.076-11.82-7.856-19.028-7.856c-7.2,0-13.96,2.78-19.02,7.856L7.872,23.988c-10.496,10.496-10.496,27.568,0,38.052	L191.828,246L7.872,429.952c-5.064,5.072-7.852,11.828-7.852,19.032c0,7.204,2.788,13.96,7.852,19.028l16.124,16.116	c5.06,5.072,11.824,7.856,19.02,7.856c7.208,0,13.968-2.784,19.028-7.856l183.96-183.952l183.952,183.952	c5.068,5.072,11.824,7.856,19.024,7.856h0.008c7.204,0,13.96-2.784,19.028-7.856l16.12-16.116	c5.06-5.064,7.852-11.824,7.852-19.028c0-7.204-2.792-13.96-7.852-19.028L300.188,246z"/></g></g></svg></div>')
            $('.v-close').click(function () {
                $('.virtual-tur').removeClass('active');
            });
        });
    }
    ;

    var pagePath = '/zhk-platonov';
    if (document.location.href.indexOf(pagePath) != -1)
    {
        function readDeviceOrientation()
        {
            // window.innerHeight is not supported by IE
            var winH = window.innerHeight ? window.innerHeight : jQuery(window).height();
            var winW = window.innerWidth ? window.innerWidth : jQuery(window).width();
            //force height for iframe usage
            if (!winH || winH == 0)
            {
                winH = '100%';
            }
            // set the height of the document
            jQuery('html').css('height', winH);
            // scroll to top
            window.scrollTo(0, 0);
        }
        jQuery(document).ready(function () {
            if (/(iphone|ipod|ipad|android|iemobile|webos|fennec|blackberry|kindle|series60|playbook|opera\smini|opera\smobi|opera\stablet|symbianos|palmsource|palmos|blazer|windows\sce|windows\sphone|wp7|bolt|doris|dorothy|gobrowser|iris|maemo|minimo|netfront|semc-browser|skyfire|teashark|teleca|uzardweb|avantgo|docomo|kddi|ddipocket|polaris|eudoraweb|opwv|plink|plucker|pie|xiino|benq|playbook|bb|cricket|dell|bb10|nintendo|up.browser|playstation|tear|mib|obigo|midp|mobile|tablet)/.test(navigator.userAgent.toLowerCase()))
            {
                if (/iphone/.test(navigator.userAgent.toLowerCase()) && window.self === window.top)
                {
                    jQuery('body').css('height', '100.18%');
                }
                // add event listener on resize event (for orientation change)
                if (window.addEventListener)
                {
                    window.addEventListener("load", readDeviceOrientation);
                    window.addEventListener("resize", readDeviceOrientation);
                    window.addEventListener("orientationchange", readDeviceOrientation);
                }
                //initial execution
                setTimeout(function () {
                    readDeviceOrientation();
                }, 10);
            }
        });

        function accessWebVr(curScene)
        {
            unloadPlayer();
            setTimeout(function () {
                loadPlayer(true, curScene);
            }, 100);
        }

        function accessStdVr(curScene)
        {
            unloadPlayer();
            setTimeout(function () {
                loadPlayer(false, curScene);
            }, 100);
        }

        function loadPlayer(isWebVr, curScene)
        {
            if (isWebVr)
            {
                embedpano({
                    id: "krpanoSWFObject2",
                    xml: "/indexdata2/index_vr.xml",
                    target: "panoDIV",
                    passQueryParameters: true,
                    bgcolor: "#000000",
                    html5: "only+webgl",
                    focus: false,
                    vars: {skipintro: true, norotation: true, startscene: curScene}
                });
            } else
            {
                var isBot = /bot|googlebot|crawler|spider|robot|crawling/i.test(navigator.userAgent);
                embedpano({
                    id: "krpanoSWFObject2",
                    swf: "/indexdata2/index.swf",
                    target: "panoDIV",
                    passQueryParameters: true,
                    bgcolor: "#000000",
                    focus: false,
                    html5: isBot ? "always" : "prefer",
                    vars: {startscene: curScene},
                    localfallback: "flash"
                });
            }
            if (top.location === self.location)
            {
                kpanotour.Focus.applyFocus();
            }
        }

        function unloadPlayer()
        {
            if (jQuery('#krpanoSWFObject2'))
            {
                removepano('krpanoSWFObject2');
            }
        }

        function isVRModeRequested()
        {
            var querystr = window.location.search.substring(1);
            var params = querystr.split('&');
            for (var i = 0; i < params.length; i++)
            {
                if (params[i].toLowerCase() == "vr")
                {
                    return true;
                }
            }
            return false;
        }

        function vStart()
        {
            if (isVRModeRequested())
            {
                accessWebVr();
            } else
            {
                accessStdVr();
            }
        }
        vStart();

        $('.v-btn2').click(function (e) {
            e.preventDefault()
            $('.virtual-tur2').addClass('active')
            $('.virtual-tur2').prepend('<div class="v-close"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"	viewBox="0 0 492 492" style="enable-background:new 0 0 492 492;" xml:space="preserve"><g><g><path d="M300.188,246L484.14,62.04c5.06-5.064,7.852-11.82,7.86-19.024c0-7.208-2.792-13.972-7.86-19.028L468.02,7.872 c-5.068-5.076-11.824-7.856-19.036-7.856c-7.2,0-13.956,2.78-19.024,7.856L246.008,191.82L62.048,7.872	c-5.06-5.076-11.82-7.856-19.028-7.856c-7.2,0-13.96,2.78-19.02,7.856L7.872,23.988c-10.496,10.496-10.496,27.568,0,38.052	L191.828,246L7.872,429.952c-5.064,5.072-7.852,11.828-7.852,19.032c0,7.204,2.788,13.96,7.852,19.028l16.124,16.116	c5.06,5.072,11.824,7.856,19.02,7.856c7.208,0,13.968-2.784,19.028-7.856l183.96-183.952l183.952,183.952	c5.068,5.072,11.824,7.856,19.024,7.856h0.008c7.204,0,13.96-2.784,19.028-7.856l16.12-16.116	c5.06-5.064,7.852-11.824,7.852-19.028c0-7.204-2.792-13.96-7.852-19.028L300.188,246z"/></g></g></svg></div>')
            $('.v-close').click(function () {
                $('.virtual-tur2').removeClass('active');
            })
        })
    }
});
$(document).ready(function () {
//    var urlString = window.location.href;     // Returns full URL (https://example.com/path/example.html)
    var getUrlParameter = function getUrlParameter(sParam) {
        var sPageURL = window.location.search.substring(1),
                sURLVariables = sPageURL.split('&'),
                sParameterName,
                i;
        for (i = 0; i < sURLVariables.length; i++) {
            sParameterName = sURLVariables[i].split('=');
            if (sParameterName[0] === sParam) {
                return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
            }
        }
        return false;
    };
    var formVar = getUrlParameter('form');
    if (formVar == 'internet-reception') {
        $('.navbar__top a[data-src="/popup/form-internet-reception-new.php"]').trigger('click')
    }
})