$(document).ready(function () {
    $('.sjow_more_plan3DItem').click(function (e) {
        e.preventDefault();
        $(this).closest('.plan3DItem_list_wr').toggleClass('active');
    });
    $('.project-detail-description').click(function(){
        $(this).toggleClass('active');
    });
    $('.tab_link').click(function(e){
        e.preventDefault();
        $(this).closest('.section-menu').find('.tab_link').removeClass('active');
        $(this).addClass('active');
        $(this).closest('.tab_wr').find('.tab_item').removeClass('active');
        $(this).closest('.tab_wr').find('.tab_item[data-tabid="'+ $(this).data('tabid')+'"]').addClass('active');
            
    })
})

