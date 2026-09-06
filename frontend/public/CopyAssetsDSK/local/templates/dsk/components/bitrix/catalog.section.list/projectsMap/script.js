$(function () {
    $(document).on('click', '.contacts-custom-point', function () {
        $('.ymaps3--marker').removeClass('_active');
        $(this).closest('.ymaps3--marker').addClass('_active');
        $('.project-map-card-wr').removeClass('_active');
        $('#' + $(this).data('id')).addClass('_active');
        // Скролл к низу #project-map
        if ($(document).width() < 820) {
            var $projectMap = $('#project-map');
            if ($projectMap.length) {
                var scrollTo = $projectMap.offset().top + $projectMap.outerHeight() - ($projectMap.outerHeight() / 2);
                $('html, body').animate({scrollTop: scrollTo}, 500);
            }
        }
    });
    $('.project-map-card-close').click(function () {
        $(this).closest('.project-map-card-wr').removeClass('_active');
        $('.ymaps3--marker').removeClass('_active');
    })
});

