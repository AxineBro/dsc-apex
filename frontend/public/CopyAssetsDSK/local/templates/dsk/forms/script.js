$(document).ready(function () {
//генерируем случайную строку
    $('.send_mail_raba input[type="tel"]').inputmask("+7 (999) 999-99-99");
    function formatDate(date) {
        let d = new Date(date),
                month = '' + (d.getMonth() + 1),
                day = '' + d.getDate(),
                year = d.getFullYear();
        if (month.length < 2)
            month = '0' + month;
        if (day.length < 2)
            day = '0' + day;
        return [day, month, year].join('.');
    }
    function makeid(length) {
        var result = '';
        var characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        var charactersLength = characters.length;
        for (var i = 0; i < length; i++) {
            result += characters.charAt(Math.floor(Math.random() * charactersLength));
        }
        return result;
    }
//генерируем случайную строку END
    $("form.send_mail_raba").append('<input type="hidden" name="clickField" class="clickField" value="' + makeid(10) + '" />');
    $("form.send_mail_raba").append('<input type="hidden" name="inputField" class="inputField" value="' + makeid(10) + '" />');
    $("body").append('\
                    <div class="response_container">\n\
                        <div class="response">\n\
                            <div class="response_wrap">\n\
                                <div class="response_close_cros"><svg width="20" height="20" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" clip-rule="evenodd" d="M10.2929 10.9203L0 21.2132L0.707092 21.9203L11 11.6274L21.2929 21.9203L22 21.2132L11.7071 10.9203L21.9203 0.707108L21.2132 0L11 10.2132L0.786804 0L0.0797119 0.707108L10.2929 10.9203Z"></path></svg></div>\n\
                                <div class="response_title">Title</div>\n\
                                <div class="response_text">text</div>\n\
                                    <div class="response_close btn--green">Закрыть</div>\n\
                            </div>\n\
                        </div>\n\
                    </div>');
    function closeForm() {
        $(".response").fadeOut(500);
        $(".response_container").fadeOut(500);
        $('.feedback-wrapper').css("display", "none");
        $('.page-blur').removeClass('page-wrap-fix');
        $('.page').css("overflow", "auto");
    }
    function clearFormZapis() {
        $('#apartment').val('');
        $('#datepicker').val('');
        $('#time_list').val('');
        $('#new_record_form input[name="familiya"]').val('');
        $('#new_record_form input[name="name"]').val('');
        $('#new_record_form input[name="otchestvo"]').val('');
    }
    $(".response_close").on('click', closeForm);
    $(".response_close_cros").on('click', closeForm);
//    $(".response_container").on('click', closeForm);

    // типа капча
    $(".send_mail_raba input").on('click', (function () {
        $(".send_mail_raba .clickField").val("click_true");
    }));
    $(".send_mail_raba input").on("keydown", function () {
        $(".send_mail_raba .inputField").val("keyBoard_true");
    });
    // типа капча END
    // отправка записи на заселение --------------------------------------------
    $("form#new_record_form").submit(function (e) {
        e.preventDefault();
        $('#print_result').html(
                '<h1>Электронная запись</h1>\n\
                <p><b>Фамилия собственника</b>: ' + $('#new_record_form input[name="familiya"]').val() + '</p>\n\
                <p><b>Имя</b>: ' + $('#new_record_form input[name="name"]').val() + '</p>\n\
                <p><b>Отчество</b>: ' + $('#new_record_form input[name="otchestvo"]').val() + '</p>\n\
                <p><b>Телефон</b>: ' + $('#new_record_form input[name="phone"]').val() + '</p>\n\
                <p><b>E-mail</b>: ' + $('#new_record_form input[name="email"]').val() + '</p>\n\
                <p><b>Номер позиции</b>: ' + $('#new_record_form input[name="number"]').val() + '</p>\n\
                <p><b>Номер квартиры</b>: ' + $('#new_record_form input[name="apartment"]').val() + '</p>\n\
                <p><b>День</b>: ' + $('#new_record_form input[name="date"]').val() + '</p>\n\
                <p><b>Время</b>: ' + $('#new_record_form select[name="time"]').val() + '</p>\n\
                ');
        $('.preloader').addClass('active');
        var form = $(this), data = new FormData(this);
        $.ajax({
            type: 'POST', url: '/local/templates/dsk/forms/ajax_zaselenie.php',
            data: data,
            cache: false, contentType: false, processData: false, mimeType: "multipart/form-data", dataType: "json",
            success: function (msg) {
                $('.preloader').removeClass('active');
                $('.fancybox-close-small').trigger('click');
                // показываем сообщение
                $(".response .response_title").html(msg.title);
                $(".response .response_text").html(msg.text);
                $(".response").fadeIn(500);
                $(".response_container").fadeIn(500);
                setTimeout(function () {
                    $(".response").fadeOut(500);
                    $(".response_container").fadeOut(500);
                }, 20000);
                clearFormZapis();
                // показываем сообщение END
            },
            error: function (xhr, str) {
                $('.preloader').removeClass('active');
                console.log('error');
                console.log(xhr);
                console.log(str);
            }
        });
    });
    // отправка записи на заселение END ----------------------------------------

    // Логика формы

    // выбор типа формы -------------------------------------------------------- 
    $('#form_zaselenie .container_chois_type_zaselenie input[name="type_zaselenie"]').change(function () {
        $('.send_mail_raba').hide();
        $('.send_mail_raba#' + $(this).attr('id') + '_form').show();
    })
    // выбор типа формы END ---------------------------------------------------- 

    // забираем номер позиции --------------------------------------------------
    $('#number_pozicii').val($('.btn_zaselenie').first().data('jk_name'));
    $('#number_pozicii_read').val($('.btn_zaselenie').first().data('jk_name'));
    $('#number_pozicii_del').val($('.btn_zaselenie').first().data('jk_name'));
    // забираем номер позиции END ----------------------------------------------

    // удаляем ноль в номере квартиры ------------------------------------------
    $('#apartment, #apartment_del, #apartment_read').change(function () {
        $(this).val($(this).val().replace(/^0+/, ''));
    })
    // удаляем ноль в номере квартиры END --------------------------------------
    // проверка номера квартиры в этом ЖК --------------------------------------
    $('#new_record_form #apartment').change(function () {
//        $('#datepicker').prop('disabled', true);
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_testFlat.php',
                {apartment: $('#apartment').val(), numberPozicii: $('#number_pozicii').val()},
                onAjaxSuccessTestFlat);
        function onAjaxSuccessTestFlat(data) {
            if (data.length <= 0) {
                $('.preloader').removeClass('active');
//                $('#datepicker').prop('disabled', false);
                return false;
            }
            var msg = $.parseJSON(data);
            $('.preloader').removeClass('active');
            $('.fancybox-close-small').trigger('click');
            // показываем сообщение
            $('.datepicker').hide();
            $('#apartment').val('');
            $(".response .response_title").html(msg.title);
            $(".response .response_text").html(msg.text);
            $(".response").fadeIn(500);
            $(".response_container").fadeIn(500);
            setTimeout(function () {
                $(".response").fadeOut(500);
                $(".response_container").fadeOut(500);
            }, 20000);
            // показываем сообщение END
        }
        $.post('/local/templates/dsk/forms/ajax_testFlatRange.php',
                {apartment: $('#apartment').val(), rec_id: $('#btn_zaselenie_building').data('rec_id'), numberPozicii: $('#number_pozicii').val()},
                onAjaxSuccessTestFlatRange);
        function onAjaxSuccessTestFlatRange(data) {
            data = $.parseJSON(data);
            console.log(data);
            if (!$.isEmptyObject(data.date)) {
                var disabledDates = $('#btn_zaselenie_building').data('disabled_date');
                if (disabledDates.length > 0) {
                    $('#btn_zaselenie_building').data('disabled_date', disabledDates + ',' + data.date);
                } else {
                    $('#btn_zaselenie_building').data('disabled_date', data.date);
                }
                initDatepicker();
            } else {
                return false;
            }
        }
    });
    function initDatepicker() {
        $('.preloader').addClass('active');
        var startDate = $('.btn_zaselenie').first().data('start_date');
        // если дата начала уже прошла - ставим текущую дату как начальную
        var startFateF = new Date(startDate.split('.')[1] + '.' + startDate.split('.')[0] + '.' + startDate.split('.')[2]);
        var currentDateF = new Date();
        if (startFateF < currentDateF) {
            startDate = formatDate(new Date());
        }
        var endDate = $('.btn_zaselenie').first().data('end_date');
        // забираем даты, у которых нет свободного времени ---------
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_getEmptyDate.php',
                {numberPozicii: $('#number_pozicii').val()},
                onAjaxSuccessTestFlat);
        function onAjaxSuccessTestFlat(data) {
            var tempDate;
            $('.preloader').removeClass('active');
            if (data.length > 0) {
                tempDate = ',' + data;
            }
            var disableDate = '[' + $('.btn_zaselenie').first().data('disabled_date') + tempDate + ']';
            //            var enabledDates = '[' + $('.btn_to_office').first().data('enabled_date') + tempDate + ']';
            var enabledDatesFromData = $('.btn_zaselenie').first().data('enabled_date') || '';
            var enabledDates = enabledDatesFromData.split(',').filter(item => item.trim() !== '');
            $('#datepicker').datepicker('remove');
            $('#datepicker').val('');
            $('#time_list').html('<option disabled></option>');
            $("#datepicker").datepicker({
                language: 'ru',
                orientation: 'top left',
                weekStart: 1,
                format: 'dd.mm.yyyy',
                startDate: startDate,
                endDate: endDate,
                datesDisabled: disableDate,
                autoclose: true,
//                daysOfWeekDisabled: [0, 6],
                daysOfWeekHighlighted: [0, 6],
                beforeShowDay: function (date) {
                    const day = String(date.getDate()).padStart(2, '0');
                    const month = String(date.getMonth() + 1).padStart(2, '0');
                    const year = date.getFullYear();
                    const dateStr = `'${day}.${month}.${year}'`;

                    const dayOfWeek = date.getDay();

                    // Если дата в enabledDates - разрешаем (даже если это выходной)
                    if (enabledDates.includes(dateStr)) {
                        return true;
                    }

                    // Отключаем все субботы и воскресенья, кроме тех что в enabledDates
                    if (dayOfWeek === 0 || dayOfWeek === 6) {
                        return false;
                    }

                    // Все рабочие дни доступны
                    return true;
                }
            });
        }
        $('.preloader').removeClass('active');
        // забираем даты, у которых нет свободного времени END ------
        // календарь END -----------------------------------------------------------
    }
    // проверка номера квартиры в этом ЖК END ----------------------------------

    // календарь ---------------------------------------------------------------
    $('.btn_zaselenie').click(function () {
        initDatepicker();
    });
    // 
    // формирование свободного времени -----------------------------------------
    $('#new_record_form #datepicker').change(function () {
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_getTime.php',
                {
                    dateRecord: $('#datepicker').val(), apartment: $('#apartment').val(),
                    rec_id: $('#btn_zaselenie_building').data('rec_id'),
                    numberPozicii: $('#number_pozicii').val()
                },
                onAjaxSuccessTestFlat);
        function onAjaxSuccessTestFlat(data) {
            $('.preloader').removeClass('active');
            $('#new_record_form #time_list').html(data);
        }
    });
    // формирование свободного времени END -------------------------------------

    // печать результата -------------------------------------------------------
    $(document.body).on('click', '#print_btn_zapis', function () {
        var divToPrint = document.getElementById('print_result');
        var newWin = window.open('', 'Печать записи');
        newWin.document.open();
        newWin.document.write('<html><body onload="window.print()">' + divToPrint.innerHTML + '</body></html>');
        newWin.document.close();
//        setTimeout(function () {
//            newWin.close();
//        }, 10);

    })
    // печать результата END ---------------------------------------------------

    // проверка ФИО ------------------------------------------------------------
    $('#new_record_form input[name="name"], #new_record_form input[name="familiya"], #new_record_form input[name="otchestvo"]').change(function () {
        if ($('#new_record_form input[name="name"]').val() && $('#new_record_form input[name="familiya"]').val() && $('#new_record_form input[name="otchestvo"]').val()) {
            $('.preloader').addClass('active');
            $.post('/local/templates/dsk/forms/ajax_testFIO.php',
                    {
                        familiya: $('#new_record_form input[name="familiya"]').val(),
                        imya: $('#new_record_form input[name="name"]').val(),
                        otchestvo: $('#new_record_form input[name="otchestvo"]').val(),
                        numberPozicii: $('#number_pozicii').val(),
                    }, onAjaxSuccessTestFlat);
            function onAjaxSuccessTestFlat(data) {
                if (data.length <= 0) {
                    $('.preloader').removeClass('active');
                    return false;
                }
                $('#new_record_form input[name="name"]').val('');
                $('#new_record_form input[name="familiya"]').val('');
                $('#new_record_form input[name="otchestvo"]').val('');
                var msg = $.parseJSON(data);
                $('.preloader').removeClass('active');
                $('.fancybox-close-small').trigger('click');
                // показываем сообщение
                $('.datepicker').hide();
                $('#apartment').val('');
                $(".response .response_title").html(msg.title);
                $(".response .response_text").html(msg.text);
                $(".response").fadeIn(500);
                $(".response_container").fadeIn(500);
                setTimeout(function () {
                    $(".response").fadeOut(500);
                    $(".response_container").fadeOut(500);
                }, 20000);
                // показываем сообщение END
            }
        }
    })
    // проверка ФИО END --------------------------------------------------------

    // чтение даты и времени записи, если забыли -------------------------------
    $('form#read_record_form').submit(function (e) {
        e.preventDefault();
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_readRecord.php',
                {apartment: $('#apartment_read').val(), numberPozicii: $('#number_pozicii_read').val()},
                onAjaxSuccessTestFlat);
        function onAjaxSuccessTestFlat(data) {
            var msg = $.parseJSON(data);
            $('.preloader').removeClass('active');
            $('.fancybox-close-small').trigger('click');
            // показываем сообщение
            $('#apartment_read').val('');
            $(".response .response_title").html(msg.title);
            $(".response .response_text").html(msg.text);
            $(".response").fadeIn(500);
            $(".response_container").fadeIn(500);
            setTimeout(function () {
                $(".response").fadeOut(500);
                $(".response_container").fadeOut(500);
            }, 20000);
            // показываем сообщение END
        }
    });
    // чтение даты и времени записи, если забыли END ---------------------------
    // Удаление записи  --------------------------------------------------------
    $('form#del_record_form').submit(function (e) {
        e.preventDefault();
        data = new FormData(this)
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_delRecord.php',
                {
                    familiya: $('#del_record_form input[name="familiya_del"]').val(),
                    imya: $('#del_record_form input[name="name_del"]').val(),
                    otchestvo: $('#del_record_form input[name="otchestvo_del"]').val(),
                    numberPozicii: $('#number_pozicii_del').val(),
                    apartment: $('#apartment_del').val(),
                    phone: $('#del_record_form input[name="phone_del"]').val(),
                },
                onAjaxSuccessTestFlat);
        function onAjaxSuccessTestFlat(data) {
            var msg = $.parseJSON(data);
            $('.preloader').removeClass('active');
            $('.fancybox-close-small').trigger('click');
            // показываем сообщение
            $('#apartment_read').val('');
            $(".response .response_title").html(msg.title);
            $(".response .response_text").html(msg.text);
            $(".response").fadeIn(500);
            $(".response_container").fadeIn(500);
            setTimeout(function () {
                $(".response").fadeOut(500);
                $(".response_container").fadeOut(500);
            }, 20000);
            // показываем сообщение END
        }
    });
    // Удаление записи END -----------------------------------------------------

    // Подписка на новости
    $('form#form_subscribe').submit(function (e) {
        e.preventDefault();
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_subscribe.php',
                {
                    user_phone: $('#user_phone_subscribe').val(),
                    user_email: $('#user_email_subscribe').val(),
                    pageRequest: $('#pageRequest_subscribe').val(),
                    clickField: $(this).find('input[name="clickField"]').val(),
                    inputField: $(this).find('input[name="inputField"]').val()
                },
                onAjaxSuccessTestFlat);
        function onAjaxSuccessTestFlat(data) {
            var msg = $.parseJSON(data);
            $('.preloader').removeClass('active');
            $('.fancybox-close-small').trigger('click');
            // показываем сообщение
            $(".response .response_title").html(msg.title);
            $(".response .response_text").html(msg.text);
            $(".response").fadeIn(500);
            $(".response_container").fadeIn(500);
            setTimeout(function () {
                $(".response").fadeOut(500);
                $(".response_container").fadeOut(500);
            }, 20000);
            // показываем сообщение END
        }
    });
//    $(document).on('click', '.juno-socail-btn-item.fancybox', function () {
//        console.log('asd13');
//        $(this).fancybox();
//        
//    });
    $('form#form_callback').submit(function (e) {
        e.preventDefault();
        $('.preloader').addClass('active');
        $.post('/local/templates/dsk/forms/ajax_callback.php',
                {
                    user_phone: $('#user_phone_callback').val(),
                    user_name: $('#user_name_callback').val(),
                    pageRequest: $('#pageRequest_form_callback').val(),
                    clickField: $(this).find('input[name="clickField"]').val(),
                    inputField: $(this).find('input[name="inputField"]').val(),
                    smartToken: $(this).find('input[name="smart-token"]').val(),
                    form_id: $(this).find('input[name="form_id"]').val(),
                    form_name: $(this).find('input[name="form_name"]').val(),
                },
                callbackDone);
        function callbackDone(data) {
            var msg = $.parseJSON(data);
            $('.preloader').removeClass('active');
            $('.fancybox-close-small').trigger('click');
            // показываем сообщение
            $(".response .response_title").html(msg.title);
            $(".response .response_text").html(msg.text);
            $(".response").fadeIn(500);
            $(".response_container").fadeIn(500);
            setTimeout(function () {
                $(".response").fadeOut(500);
                $(".response_container").fadeOut(500);
            }, 20000);
            // показываем сообщение END
        }
    });
}
);
