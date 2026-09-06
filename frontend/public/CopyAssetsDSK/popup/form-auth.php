<div class="box-popup">
	<a title="Закрыть" class="popup__close icomoon" href="javascript:;" data-fancybox-close="true"></a>
	<div class="box-popup__heading mb-5">Войти в личный кабинет</div>
	<form class="form-popup form-auth" name="form-auth">
		<input type="hidden" name="AUTH_FORM" value="Y" />
		<input type="hidden" name="TYPE" value="AUTH" />
					<input type="hidden" name="backurl" value="/popup/form-auth.php" />
						<div class="form-group row align-items-center">
			<div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">E-mail</div>
			<div class="col col-12 col-md-8">
				<input class="form-control" type="text" name="USER_LOGIN" id="USER_LOGIN" maxlength="255" value="" />
			</div>
		</div>
		<div class="form-group row align-items-center">
			<div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Пароль</div>
			<div class="col col-12 col-md-8">
				<input class="form-control" type="password" name="USER_PASSWORD" id="USER_PASSWORD" maxlength="255" autocomplete="off" />
			</div>
		</div>
		<div class="form-captcha d-none">
			<div class="form-group row align-items-center">
				<div class="col col-12 col-md-4"></div>
				<div class="col col-12 col-md-8 text-center">
					<input type="hidden" name="captcha_sid" value="" />
					<img class="img-captcha img-fluid" src="/bitrix/tools/captcha.php?captcha_sid=" width="180" height="40" alt="CAPTCHA" />
				</div>
			</div>
			<div class="form-group row align-items-center">
				<div class="col col-12 col-md-4 mb-1 mb-md-0 text-blue">Введите слово на картинке</div>
				<div class="col col-12 col-md-8">
					<input class="form-control" type="text" name="captcha_word" id="captcha_word" maxlength="50" value="" size="15" autocomplete="off" />
				</div>
			</div>
		</div>
		<div class="form-group justify-content-md-end row align-items-center mb-md-5">
			<div class="col col-12 col-md-auto text-center text-md-right order-md-2 mb-4 mb-md-0">
				<button class="btn btn-primary px-5" type="submit" name="Login" value="Войти">Войти</button>
			</div>
			<div class="col col-12 col-md-auto order-md-1 pr-md-5">
									<a class="text-orange" href="javascript:;" data-src="/popup/form-registration.php">Регистрация</a><br />
								<a class="text-orange" href="/auth/?forgot_password=yes&amp;backurl=%2Fpersonal%2F">Забыли свой пароль?</a>
			</div>
		</div>
	</form>
</div>
<script type="text/javascript">
jQuery(function($) {
	var $form = $('.form-auth');
	$form.find('a[data-src]').on('click', function() {
		$.post(
			$(this).data('src'),
			{},
			function(data) {
				$.fancybox.getInstance().setContent($.fancybox.getInstance().current, data);
			},
			'html'
		);
		return false;
	});
	$form.find('button').on('click', function() {
		$form.find('button').attr('disabled', true);
		$.post(
			'/popup/form-auth.php',
			$form.serialize()+'&ajax_post=Y',
			function(data) {
				console.log(data);
				$form.find('.form-control').removeClass('error').tooltip('dispose');
				if(data.ERROR)
				{
					for(var i in data.ERRORS)
					{
						$form.find('input[name="'+i+'"]')
							.addClass('error')
							.tooltip({
								title:data.ERRORS[i],
								trigger:'focus',
								container:'.box-popup'
							})
							.tooltip('show');
					}
					$form.find('input[name="captcha_sid"]').val(data.CAPTCHA_CODE);
					$form.find('.img-captcha').attr('src', '/bitrix/tools/captcha.php?captcha_sid='+data.CAPTCHA_CODE);
					if(data.CAPTCHA_CODE)
						$form.find('.form-captcha').removeClass('d-none');
				}
				else
				{
					$form.find('.form-control').val('');
					location.href = '/personal/';
				}
				$form.find('button').attr('disabled', false);
			},
			'json'
		);
		return false;
	});
});
</script>
