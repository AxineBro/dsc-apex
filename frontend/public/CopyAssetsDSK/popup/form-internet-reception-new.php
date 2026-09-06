<div class="box-popup container">
    <a title="Закрыть" class="popup__close icomoon" href="javascript:;" data-fancybox-close="true"></a>
    <div class="box-popup__heading">Интернет-приемная</div>
    <p>
        Если у вас возникли вопросы, жалобы или предложения, - воспользуйтесь формой обратной связи
    </p>
    <form class="form-popup form-internet-reception send_mail_raba" name="form-internet-reception">
        <input class="captcha_msg" type="text" name="captcha_msg">
        <div class="form-group row align-items-center">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">ФИО</div>
            <div class="col col-12 col-md-8">
                <input class="form-control" type="text" name="user_name" id="user_name" value="" placeholder="Иванов Иван Иванович" />
            </div>
        </div>
        <div class="form-group row align-items-center">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Телефон</div>
            <div class="col col-12 col-md-8">
                <input class="form-control" type="tel" name="user_phone" id="user_phone" value="" placeholder="+7 000 000-00-00" />
            </div>
        </div>
        <div class="form-group row align-items-center">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">E-mail</div>
            <div class="col col-12 col-md-8">
                <input class="form-control" type="email" name="user_email" id="user_email" value="" placeholder="mail@mail.ru" />
            </div>
        </div>
        <div class="form-group row align-items-center">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Тема обращения</div>
            <div class="col col-12 col-md-8">
                <select class="custom-select" name="subject" id="subject">
                    <option value="">Выбрать тему</option>
                                            <option value="review">Оставить отзыв</option>
                                            <option value="request">Подать заявку на устранение строительных недостатков</option>
                                            <option value="comment">Оставить комментарий о выполненной работе по заявке на устранение строительных недостатков</option>
                                            <option value="suggestion">Внести предложение</option>
                                            <option value="gratitude">Благодарность</option>
                                            <option value="project_doc">Запрос проектной документации</option>
                                    </select>
            </div>
        </div>
        <div class="form-group form-group-ext form-group-warranty-obligations d-none mt-4 mb-3">
            <div class="custom-control custom-checkbox">
                <input type="checkbox" class="custom-control-input" name="warranty_obligations" id="warranty_obligations" value="Y" />
                <label class="custom-control-label text-gray" for="warranty_obligations">До отправления заявки обязательно <a href="/upload/information_guarantee.pdf" target="_black">ознакомьтесь</a> с действующими сроками гарантийных обязательств застройщика.</label>
            </div>
        </div>
        <div class="form-group form-group-ext form-group-address row align-items-center d-none mb-3">
            <div class="col col-12 mb-1 text-blue">Пожалуйста укажите Ваш адрес</div>
            <div class="col col-12 col-md-4 mb-2">
                <input class="form-control" type="text" name="street" id="street" value="" placeholder="Улица" />
            </div>
            <div class="col col-12 col-md-4 mb-2">
                <input class="form-control" type="text" name="building" id="building" value="" placeholder="Дом" />
            </div>
            <div class="col col-12 col-md-4 mb-2">
                <input class="form-control" type="text" name="apartment" id="apartment" value="" placeholder="Квартира" />
            </div>
        </div>
        <div class="form-group form-group-ext form-group-contract form-group-contract-cut row d-none mb-4">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Прикрепить договор</div>
            <div class="col col-12 col-md-8">
                <div class="form-file-contract d-inline-block mt-md-1">
                    <div class="file-input">
    <ol class="webform-field-upload-list mb-0" id="file_input_upload_list_mfihIKU2" style="display: none;"></ol>
            <div class="webform-field-upload">
            <span class="webform-small-button webform-button-upload">Прикрепить файлы</span>
                            <input type="file" name="contract_rMHduMTY[]" size="1" multiple="multiple" id="file_input_mfihIKU2" />
                    </div>
    </div>
<script type="text/javascript">
    BX.message({MFI_CONFIRM: 'Удалить файл?'});
    window.FILE_INPUT_mfihIKU2 = new BX.CFileInput('mfihIKU2', 'contract', '7937f61402f4e94f5c8db2ecc82dba04', '/popup/form-internet-reception-new.php', true);
</script>                    <div class="file_upload_desc">Объем файлов не должен превышать 20Мб</div>
                </div>
                <p class="mt-1 mb-0 box-popup__none">Первая и последняя страницы с подписью и реквизитами сторон</p>
                <div class="contract-chois-wr">
                    <label>
                        <input type="radio" name="contract-type" value="Договор купли-продажи" required>
                        Договор купли-продажи
                    </label>
                    <div>
                        <label>
                            <input id="contract-ddu" type="radio" name="contract-type" value="Договор ДДУ" required>
                            Договор ДДУ
                        </label>
                        <div class="contract-chois-sub-wr">
                            <label>
                                <input type="radio" name="signing-act" value="Договор ДДУ с подписанным актом приема-передачи квартиры">
                                Договор ДДУ с подписанным актом приема-передачи квартиры
                            </label>
                            <label>
                                <input type="radio" name="signing-act" value="Договор ДДУ без подписанного акта приема-передачи квартиры">
                                Договор ДДУ без подписанного акта приема-передачи квартиры
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="form-group form-group-ext form-group-contract d-none row">
            <div class="col col-12 col-md-4 mt-md-2 mb-1 mb-md-0 pt-md-1 text-blue">Пожалуйста, подробно опишите суть проблемы</div>
            <div class="col col-12 col-md-8">
                <textarea class="form-control" name="request_msg" id="user_message_contract"></textarea>
            </div>
        </div>
        <div class="form-group form-group-ext form-group-contract row d-none mb-4">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Приложите фото</div>
            <div class="col col-12 col-md-8">
                <div class="form-file-request_photo d-inline-block mt-md-1">
                    <div class="file-input">
    <ol class="webform-field-upload-list mb-0" id="file_input_upload_list_mfi8HRlC" style="display: none;"></ol>
            <div class="webform-field-upload">
            <span class="webform-small-button webform-button-upload">Прикрепить файлы</span>
                            <input type="file" name="request_photo_2V9n8peg[]" size="1" multiple="multiple" id="file_input_mfi8HRlC" />
                    </div>
    </div>
<script type="text/javascript">
    BX.message({MFI_CONFIRM: 'Удалить файл?'});
    window.FILE_INPUT_mfi8HRlC = new BX.CFileInput('mfi8HRlC', 'request_photo', '8b7bf6c0c738d1713e744f05296206d9', '/popup/form-internet-reception-new.php', true);
</script>                    <div class="file_upload_desc">Объем файлов не должен превышать 20Мб</div>
                </div>
            </div>
        </div>

        <div class="form-group form-group-ext form-group-request-number row align-items-center d-none">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Номер заявки</div>
            <div class="col col-12 col-md-8">
                <input class="form-control" type="text" name="request_number" id="request_number" value="" />
            </div>
        </div>
        <div class="form-group form-group-ext form-group-request-date row align-items-center d-none">
            <div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Дата зарегистрированной заявки</div>
            <div class="col col-12 col-md-8">
                <input onclick="BX.calendar({node: this, field: this, bTime: true});" class="form-control" type="text" name="request_date" id="request_date" value="" placeholder="дд.мм.гггг" />
            </div>
        </div>
        <div class="form-group form-group-ext form-group-comment row d-none">
            <div class="col col-12 col-md-4 mt-md-2 mb-1 mb-md-0 pt-md-1 text-blue">Комментарий о выполненной работе</div>
            <div class="col col-12 col-md-8">
                <textarea class="form-control" name="user_comment" id="user_comment"></textarea>
            </div>
        </div>
        <div class="form-group form-group-ext form-group-message row d-none">
            <div class="col col-12 col-md-4 mt-md-2 mb-1 mb-md-0 pt-md-1 text-blue">Текст сообщения</div>
            <div class="col col-12 col-md-8">
                <textarea class="form-control" name="user_message" id="user_message"></textarea>
            </div>
        </div>
        <div class="form-group form-group-ext form-group-file row d-none mb-4">
            <div class="col col-12 col-md-4 mt-md-1 mb-1 mb-md-0 text-blue">Прикрепить файл</div>
            <div class="col col-12 col-md-8">
                <div class="form-file-file d-inline-block mt-md-1">
                    <div class="file-input">
    <ol class="webform-field-upload-list mb-0" id="file_input_upload_list_mfixCNcl" style="display: none;"></ol>
            <div class="webform-field-upload">
            <span class="webform-small-button webform-button-upload">Прикрепить файл</span>
                            <input type="file" name="file_QsgV5Uz5[]" size="1" multiple="multiple" id="file_input_mfixCNcl" />
                    </div>
    </div>
<script type="text/javascript">
    BX.message({MFI_CONFIRM: 'Удалить файл?'});
    window.FILE_INPUT_mfixCNcl = new BX.CFileInput('mfixCNcl', 'file', '07a8c438cb87a0a6cfe69f068c97b5ba', '/popup/form-internet-reception-new.php', true);
</script>                    <div class="file_upload_desc">Объем файлов не должен превышать 20Мб</div>
                </div>
            </div>
        </div>
        <div class="form-group row align-items-center">
            <div class="col col-12 col-md-12 mb-1 mb-md-0 box-popup__none">
                <label style="display: flex;align-items: center;gap: 8px;">
                    <input type="checkbox" name="agree" value="Y" >
                    <span>Даю <a class="text-orange" href="/upload/consent.docx" target="_blank">согласие</a> на обработку своих персональных данных.</span>
                </label>
            </div>
        </div>
        <div class="form-group mb-6 text-right">
            <button class="btn btn-primary px-5" type="submit" name="send" value="Отправить">Отправить</button>
        </div>
        <div class="box-popup__none">
            Ваше обращение считается зарегистрированным после получения Вами подтверждения
            регистрации (номер, дата обращения) на адрес Вашей электронной почты.
        </div>
    </form>
</div>
<script type="text/javascript">
    jQuery(function ($) {
        //генерируем случайную строку
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
        // типа капча
        $(document.body).on('click', ".send_mail_raba input", function () {
            $(".send_mail_raba .clickField").val("click_true");
        });
        $(document.body).on('keydown', ".send_mail_raba input", function () {
            $(".send_mail_raba .inputField").val("keyBoard_true");
        });
        // типа капча END
        $("form.send_mail_raba").append('<input type="hidden" name="clickField" class="clickField" value="' + makeid(10) + '" />');
        $("form.send_mail_raba").append('<input type="hidden" name="inputField" class="inputField" value="' + makeid(10) + '" />');

        var $form = $('.form-internet-reception');
        $form.find('input[name="user_phone"]').inputmask("+7 (999) 999-99-99");
        $form.find('#subject').on('change', function () {
            $form.find('.form-group-ext').addClass('d-none');
            $form.find('.form-control, .custom-control-input, .custom-select, .form-file-contract, .form-file-request_photo').removeClass('error').tooltip('dispose');
            if (
                    $(this).val() == 'review' ||
                    $(this).val() == 'suggestion' ||
                    $(this).val() == 'gratitude'
                    )
            {
                $('.form-group-message').removeClass('d-none');
                $('.form-group-file').removeClass('d-none');
            } else if ($(this).val() == 'request')
            {
                $('.form-group-warranty-obligations').removeClass('d-none');
                $('.form-group-address').removeClass('d-none');
                $('.form-group-contract').removeClass('d-none');
            } else if ($(this).val() == 'comment')
            {
                $('.form-group-request-number').removeClass('d-none');
                $('.form-group-request-date').removeClass('d-none');
                $('.form-group-comment').removeClass('d-none');
            } else if ($(this).val() == 'project_doc')
            {
                $('.form-group-message').removeClass('d-none');
                $('.form-group-contract-cut').removeClass('d-none');
                $('.form-group-address').removeClass('d-none');
            }
        });
        $form.find('button').on('click', function () {
            $form.find('button').attr('disabled', true);
            $.post(
                    '/popup/form-internet-reception-new.php',
                    $form.serialize() + '&ajax_post=Y',
                    function (data) {
                        $form.find('.form-control, .custom-control-input, .custom-select, .form-file-contract, .form-file-request_photo').removeClass('error').tooltip('dispose');
                        if (data.OK) {
                            data.subjectVal = $form.find('#subject').val();
                            $form.find('.form-control').val('');
                            $.post(
                                    '/popup/form-internet-reception-success.php',
                                    data, function (data) {
                                        $.fancybox.getInstance().setContent($.fancybox.getInstance().current, data);
                                    }, 'html'
                                    );
                        } else {
                            for (var i in data.ERRORS) {
                                $form.find('input[name="' + i + '"], select[name="' + i + '"], textarea[name="' + i + '"], .form-file-' + i)
                                        .addClass('error')
                                        .tooltip({
                                            title: data.ERRORS[i],
                                            trigger: 'focus',
                                            container: '.form-internet-reception'
                                        })
                                        .tooltip('show');
                            }
                        }
                        $form.find('button').attr('disabled', false);
                    }, 'json', );
            return false;
        });
        // Тип договора батоны
        $('[name="contract-type"]').change(function () {
            if ($('#contract-ddu').is(':checked')) {
                $('.contract-chois-sub-wr').addClass('active');
                $('.contract-chois-sub-wr input[type="radio"]').prop("required", true);
            } else {
                $('.contract-chois-sub-wr').removeClass('active');
                $('.contract-chois-sub-wr input[type="radio"]').prop("required", false);
            }
        });
    });
</script>
<style>
    .contract-chois-wr{
        font-size: 15px;
        margin-top: 15px;
        /*margin-left: -140px;*/
    }
    .contract-chois-wr label{
        display: flex;
        gap: 15px;
        align-items: baseline;
        line-height: 1.3;
    }
    .contract-chois-sub-wr{
        margin-left: 30px;
    }
    .contract-chois-sub-wr{
        max-height: 0;
        overflow: hidden;
        transition: .3s;
    }
    .contract-chois-sub-wr.active{
        max-height: 200px;
    }
</style>