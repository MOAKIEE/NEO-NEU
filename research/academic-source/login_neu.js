document.body.onload=a;

$(function() {

	//发送手机验证码
	$("#sendCode").click(function(){
		var u = $("#un").val() ;
		var p = $("#phone").val() ;
		if(!u){
			layer.msg("用户名不能为空");
			return ;
		}
		if(!p){
			layer.msg("手机号不能为空  ");
			return ;
		}
		$.post("device" , {
			m : '2' ,
		} , function(ret){
			if(ret.info == 'max'){
				layer.msg("发送过于频繁，请稍后再试");
			}else if(ret.info == 'unknow'){
				layer.msg("当前手机尚未绑定");
			}else if(ret.info == 'send'){
				$("#sendCode").hide();
				var count = 300 ;
				$("#timeout").show().text(count + " s");
				var t1 = window.setInterval(function(){
					$("#timeout").text(count+" s");
					count-- ;
					if(count <= 0){
						window.clearInterval(t1);
						$("#timeout").hide();
						$("#sendCode").show();
					}
				},1000);
			}
		} , 'json');
	});

});

function a(){
//	if(window.localStorage){
//		//重新登录的时候清除掉localStorage
//		window.localStorage.clear();
//	}
//	if(window.sessionStorage){
//		//重新登录的时候清除掉sessionStorage
//		window.sessionStorage.clear();
//	}
	var passwordhtml = document.getElementById("password_template").innerHTML;
	//初始化点击事件
	initPassWordEvent();
	//判断问题反馈显示样式
	var cookie_up_feedback = getCookie('up_feedback');
	if(cookie_up_feedback==1){
		$(".new-question ").removeClass("hide-feedback");
	}else{
		$(".new-question ").addClass("hide-feedback");
	}
} 

function login(){
	var $u = $("#un") , $p=$("#pd");
	
	var u = $u.val();
	if(u==""){
		$u.focus();
		$u.parent().addClass("login_error_border");
		return ;
	}
	
	var p = $p.val();
	if(p==""){
		$p.focus();
		$p.parent().addClass("login_error_border");
		return ;
	}
	
	$u.attr("disabled","disabled");
	$p.attr("disabled","disabled");
	
	var lt = $("#lt").val();
	
	$("#ul").val(u.length);
	$("#pl").val(p.length);
//	$("#rsa").val(u+p+lt);
	const publicKeyStr = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnjA28DLKXZzxbKmo9/1WkVLf1mr+wtLXLXt6sC4WiBCtsbzF5ewm7ARZeAdS3iZtqlYPn6IcUoOw42H8nAK/tfFcIb6dZ1K0atn0U39oWCGPzYuKtLJeMuNZiDXVuAXtojrckOjLW9B3gUnaNGLuIx0fYe66l0o9WjU2cGLNZQfiIxs2h00z1EA9IdSnVxiVQWSD+lsP3JZXh2TT287la4Y4603SQNKTK/QvXfcmccwTEd1IW6HwGxD6QrkInBiHisKWxmveN7UDSaQRZ/J97G0YC32pD38WT53izXeK0p/kU/X37VP555um1wVWFvPIuc9I7gMP1+hq5a+X6c++tQIDAQAB";
	const rsa = new RSAEncryptor(publicKeyStr);
	var originalData = rsa.encrypt(u+p);
	
	$("#rsa").val(originalData);
	
	$("#loginForm")[0].submit();
}
//初始化登录页事件
function initPassWordEvent(){
	$("[name='new_question']").click(function(){
		window.open("https://portal.neu.edu.cn/tp_up/view?m=up#act=up/feedback/feedback_new");
	});
	$(".new-question-close").click(function(){
		$(".new-question").addClass("hide-feedback");
		setCookie("up_feedback","0","365");
	});
	$("#retract").click(function(){
		$(".new-question ").removeClass("hide-feedback");
		setCookie("up_feedback","1","365");
	});
	var passwordhtml = document.getElementById("password_template").innerHTML;
	var qrcodehtml = document.getElementById("qrcode_template").innerHTML;
	var loginByMobile_template = document.getElementById("mobile_template").innerHTML;
	$("#index_login_btn").click(function(){
		login();
	}); 
	//点击记住用户名
	$("#rememberName").change(function(){
		if($(this).is(":checked")){
			var $u = $("#un").val() ;
			if($.trim($u)==''){
				$("#errormsg").text("账号不能为空。").show();
				$(this).removeAttr("checked");
			}else{
				//不等于空，写cookie
				setCookie('neu_cas_un' , $u , 365);
			}
		}else{
			//反选之后清空cookie
			clearCookie('neu_cas_un');
		}
	});

	//用户名文本域keyup事件
	$("#un").keyup(function(e){
		if(e.which == 13) {
			login();
	    }
	}).keydown(function(e){
		$("#errormsg").hide();
	}).focus();
	
	//密码文本域keyup事件
	$("#pd").keyup(function(e){
		if(e.which == 13) {
			login();
	    }
	}).keydown(function(e){
		$("#errormsg").hide();
	});
	
	//如果有错误信息，则显示
	if($("#errormsghide").text()){
		$("#errormsg").text($("#errormsghide").text()).show();
	}
	//获取cookie值
	var cookie = getCookie('neu_cas_un');
	if(cookie){
		$("#un").val(cookie);
		$("#rememberName").attr("checked","checked");
	}
	//重新获取验证码
	$("#codeImage").click(function(){
    	$("#codeImage").attr("src", "code?"+Math.random()) ;
    });
	//点击账号登陆
	$("#password_login").click(function(){
		$("#password_login").addClass("active");
		$("#qrcode_login").removeClass("active");
		$("#loginByMobile").removeClass("active");
		$("#login_content").empty();
		$("#login_content").html(passwordhtml);
		initPassWordEvent();
	});
	//点击扫码登陆
	$("#qrcode_login").click(function(){
		$("#password_login").removeClass("active");
		$("#qrcode_login").addClass("active");
		$("#loginByMobile").removeClass("active");
		$("#login_content").empty();
		$("#login_content").html(qrcodehtml);
		//微信企业号扫码登录 add by TJL
		var lqrcode = new loginQRCode("qrcode",143,143);
		lqrcode.generateLoginQRCode(function(result){
			window.location.href = result.redirect_url;
		});
		//点击账号登陆
		$("#password_login").click(function(){
			$("#password_login").addClass("active");
			$("#qrcode_login").removeClass("active");
			$("#loginByMobile").removeClass("active");
			$("#login_content").empty();
			$("#login_content").html(passwordhtml);
			// initPassWordEvent();
		});
		initPassWordEvent();
	});

	// 手机验证码登录
	$("#loginByMobile").unbind().click(function(){
		$("#errormsghide").text('');
		$("#errormsg").hide();
		$("#password_login").removeClass("active");
		$("#qrcode_login").removeClass("active");
		$("#loginByMobile").addClass("active");
		$("#login_content").empty();
		$("#login_content").html(loginByMobile_template);
		initPassWordEvent();
		loginByMobile();
		$(this).unbind();
	});

	//触发如何使用360极速模式图片
	$("#open_360").mouseover(function(){
		$("#open_360_img").show();
	}).mouseout(function(){
		$("#open_360_img").hide();
	});
}

function loginByMobile(){
	document.cookie = "loginType" + "=" + "loginByMobile" + "; " ;

	//手机号文本域keyup事件
	$("#loginMobile").keyup(function(e){
	}).keydown(function(e){
		$("#errormsg").hide();
	}).focus();

	//图形验证码文本域keyup事件
	$("#sendConfirm").keyup(function(e){
	}).keydown(function(e){
		$("#errormsg").hide();
	});
	//节日初始化
	// showFestival("tip");
	//初始化提示信息
	// forgetInputAlert();

	// 绑定点击事件返回登录页
	/*$("a[name='returnLoginOne']").unbind("click").bind("click",function(){
		accountlist = null;
		clearCookie("loginType");
		home();
		$("#errormsg").removeClass("error").addClass("small-big-small").html("<span class=\"text-bold\">嗨</span>，您好 ！");
	});*/

	//重新获取验证码
	$("#codeImage").attr("src", "code?"+Math.random()) ;
	$("#codeImage").unbind("click").bind("click",function(){
		$("#codeImage").attr("src", "code?"+Math.random()) ;
	});

	//手机正则表达[1开头11位]
	let mreg = /^0?1\d{10}$/;
	// $("#loginMobile").blur(function(){
	// 	let mobile = $("#loginMobile").val();
	// 	if($.trim(mobile) == ""||!mreg.test(mobile)){
	// 		$("#errormsg").text("手机号格式错误");
	// 		toAlertInfo();
	// 	}
	// });
	//验证码相关的校验
	$("#getMobileVerifyCode").unbind("click").bind("click", function () {
		if($(this).html().indexOf($("#getPhoneCodeText").text())===-1){
			return;
		}
		//需要填写手机号码才能发送验证码
		let mobile = $("#loginMobile").val();
		let sendConfirm = $("#sendConfirm").val();
		if($.trim(mobile) == ""||!mreg.test(mobile)){
			$("#errormsg").text("手机号格式错误");
			toAlertInfo();
			return false;
		}
		else if($.trim(sendConfirm) == ""){
			$('#errormsg').text("验证码不能为空");
			toAlertInfo();
			return false;
		}
		else{
			$.post("loginByMorE", {
				"method": "sendMobileCode",
				"sendConfirm": sendConfirm,
				"mobile": mobile,
				"random": Math.random()
			}, function (data) {
				if (data.result == "false") {
					$("#codeImage").trigger("click");
					$("#errormsg").text(data.error);
					toAlertInfo();
				} else {
					//先禁用按钮以避免连续重复提交
					$("#getMobileVerifyCode").attr("disabled", "disabled");
					$("#errormsg").text("发送成功，验证码有效期5分钟，请注意查收");
					toAlertInfo();
					const intDiff = parseInt(60);//倒计时总秒数量
					dglogintimer(intDiff);
				}
			});
		}
	});
	$("#finishloginbymobile").click(function(){
		//验证手机号码
		let mobile = $("#loginMobile").val();
		if($.trim(mobile) == ""||!mreg.test(mobile)){
			$('#errormsg').text("手机号格式错误");
			toAlertInfo();
			return false;
		}
		//验证手机动态验证码
		let mobilecode = $("#phoneCode").val();
		if($.trim(mobilecode) == ""||mobilecode.length!=6){
			$('#errormsg').text("动态验证码错误");
			toAlertInfo();
			return false;
		}
		$.post("loginByMorE", {
			"method": "login",
			"mobile": mobile,
			"mobileCode": mobilecode,
			"random": Math.random(),
			"service": $.getUrlParam("service")
		}, function (data) {
			//选择登录账号
			if (Boolean(data.numberlist)) {
				accountlist = data.numberlist;
				let accountHtml = document.getElementById("chooseloginid").innerHTML;
				$("#accountForm").html(accountHtml);
				let icon = "01";
				for (let i = 0; i < accountlist.length; i++) {
					let id_number = accountlist[i].ID_NUMBER;
					let codename = accountlist[i].CODENAME;
					let unitname = accountlist[i].UNIT_NAME;
					switch (accountlist[i].CODENAME) {
						case "本科生":
							icon = "01";
							break;
						case "教工":
							icon = "02";
							break;
						case "公众用户":
							icon = "03";
							break;
						case "家长":
							icon = "04";
							if (Boolean(accountlist[i].CHILD_USER_NAME)) {
								codename = accountlist[i].CHILD_USER_NAME + " " + accountlist[i].CODENAME
							}
							break;
						case "其他人员":
							icon = "05";
							break;
						case "企业用户":
							icon = "06";
							break;
						default:
							icon = "05";
					}
					$("#numberlist").append('<div class="popup-list cl-after list-' + icon + '" name="numbercard" data-value="' + i + '">' +
						'<div class="list-icon pull-left">' +
						'<img src="comm/image/bg/popup-list-' + icon + '.png" alt="">' +
						'</div>' +
						'<div class="list-text pull-left">' +
						'<p class="job">' + codename + ' ' + id_number + '</p>' +
						'<p class="add">' + unitname + '</p>' +
						'</div>' +
						'</div>'
					);
					$("#numberlist").find("div[name='numbercard']").each(function () {
						$(this).unbind().click(function () {
							let index = $(this).attr("data-value");
							let idnumber = accountlist[index].ID_NUMBER;
							layer.confirm('确认登录账号[' + idnumber + ']？', {
								title: "登录确认",
								time: 0 //不自动关闭
								, btn: ['确认', '取消']
								, yes: function (index) {
									layer.close(index);
									loginBymobileOnChooseNumber(mobile, mobilecode, idnumber);
								}
							});
							return;
						})
					});
				}
				$(".scroll-list").mCustomScrollbar({
					axis: "y", // horizontal scrollbar
					alwaysShowScrollbar: 0,
					theme: "minimal-dark"
				});
				$("#closecardlist").click(function () {
					$("#accountForm").html("");
				});
				return false;
			}
			if (data.result == "false") {
				$('#errormsg').text(data.error);
				toAlertInfo();
			} else if (!Boolean(data.redirectUrl)) {
				window.location.href = "login";
			} else {
				// if (data.verified) {
				// 	//跳转实名认证
				// 	layer.confirm('是否跳转省统一身份认证系统进行实名认证?', {btn: ['确定', '取消'], title: "实名认证"}, function () {
				// 		window.location.href = data.redirectUrl;
				// 	});
				// } else {
				//正常登录
				window.location.href = data.redirectUrl;
				// }
			}
		});
	});
}

function loginBymobileOnChooseNumber(mobile, mobilecode, account) {
	$.post("loginByMorE", {
		"method": "login",
		"mobile": mobile,
		"mobileCode": mobilecode,
		"random": Math.random(),
		"service": $.getUrlParam("service"),
		"account": account
	}, function (data) {
		if (data.result == "false") {
			$('#errormsg').text(data.error);
			toAlertInfo();
		} else if (!Boolean(data.redirectUrl)) {
			window.location.href = "login";
		} else {
			// if (data.verified) {
			// 	//跳转实名认证
			// 	layer.confirm('是否跳转省统一身份认证系统进行实名认证?', {btn: ['确定', '取消'], title: "实名认证"}, function () {
			// 		window.location.href = data.redirectUrl;
			// 	});
			// } else {
			//正常登录
			window.location.href = data.redirectUrl;
			// }
		}
	});
}

function getParameter(hash,name,nvl) {
	if(!nvl){
		nvl = "";
	}
	var svalue = hash.match(new RegExp("[\?\&]?" + name + "=([^\&\#]*)(\&?)", "i"));
	if(svalue == null){
		return nvl;
	}else{
		svalue = svalue ? svalue[1] : svalue;
		svalue = svalue.replace(/<script>/gi,"").replace(/<\/script>/gi,"").replace(/<html>/gi,"").replace(/<\/html>/gi,"").replace(/alert/gi,"").replace(/<span>/gi,"").replace(/<\/span>/gi,"").replace(/<div>/gi,"").replace(/<\/div>/gi,"");
		return svalue;
	}
}

//设置cookie
function setCookie(cname, cvalue, exdays) {
  var d = new Date();
  d.setTime(d.getTime() + (exdays*24*60*60*1000));
  var expires = "expires="+d.toUTCString();
  document.cookie = cname + "=" + cvalue + "; " + expires;
}

//获取cookie
function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for(var i=0; i<ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0)==' ') c = c.substring(1);
      if(c.indexOf(name)!=-1&&name=="up_feedback") return c.substring(name.length, c.length);
      if (c.indexOf(name) != -1) return c.substring(name.length, c.length);
  }
  return "";
}

//清除cookie  
function clearCookie(name) {  
  setCookie(name, "", -1);  
}

// 弹出提示信息
function toAlertInfo() {
	$("#errormsg").show();
}

function dglogintimer(intDiff){
	let showname = $('#getMobileVerifyCode').html();
	$("#getMobileVerifyCode").unbind("click");
	let second = intDiff;
	if(intDiff < 10){
		second = "0" + intDiff;
	}
	$('#getMobileVerifyCode').html(second + "秒后重新获取");//60秒后重新获取
	intDiff--;
	logtimer=window.setInterval(function(){
		var second=0;//时间默认值
		if(intDiff > 0){
			second = Math.floor(intDiff);
		}
		if (second <= 9){
			second = '0' + second;
		}

		$('#getMobileVerifyCode').html(second+"秒后重新获取");//'秒后重新获取'
		if(second == 0){
			window.clearInterval(logtimer);
			$('#getMobileVerifyCode').html(showname);
			//获取手机验证码
			$("#getMobileVerifyCode").unbind("click").bind("click", function () {
				if($(this).html().indexOf($("#getPhoneCodeText").text())===-1){
					return;
				}
				//需要填写手机号码才能发送验证码
				let mobile = $("#loginMobile").val();
				let sendConfirm = $("#sendConfirm").val();
				if($.trim(mobile) == ""||!mreg.test(mobile)){
					$("#errormsg").text("手机号格式错误");
					toAlertInfo();
					return false;
				}
				else if($.trim(sendConfirm) == ""){
					$('#errormsg').text("验证码不能为空");
					toAlertInfo();
					return false;
				}
				else{
					$.post("loginByMorE", {
						"method": "sendMobileCode",
						"sendConfirm": sendConfirm,
						"mobile": mobile,
						"random": Math.random()
					}, function (data) {
						if (data.result == "false") {
							$("#codeImage").trigger("click");
							$("#errormsg").text(data.error);
							toAlertInfo();
						} else {
							//先禁用按钮以避免连续重复提交
							$("#getMobileVerifyCode").attr("disabled", "disabled");
							const intDiff = parseInt(60);//倒计时总秒数量
							dglogintimer(intDiff);
						}
					});
				}
			});
			$("#getMobileVerifyCode").removeAttr("disabled");
		}
		intDiff--;
	}, 1000);
}

function phone(murmur_s , details_s){
	var win = layer.open({
		type : 1 ,
		title : "二次认证 " ,
		content : $("#template_phone") ,
		area : ['410px' , '250px'] ,
	});

	$("#second_valid_ok").unbind("click").click(function(){
		var u = $("#un").val() ;
		var c = $("#mcode").val() ;
		if(!u){
			layer.msg("用户名不能为空");
			return ;
		}
		if(!c){
			layer.msg("验证码不能为空  ");
			return ;
		}
		$("#second_valid_ok").prop("disabled" , "disabled");
		$.post("device" , {
			d : murmur_s ,
			i : details_s ,
			m : '3' ,
			u : u ,
			c : c ,
			s : $("#saveDevice").prop("checked") ? 1 : 0
		} , function(ret){
			$("#second_valid_ok").prop("disabled" , false);
			if(ret.info == 'most'){
				layer.msg("设备已经超过最大数量,已自动解除最早一台授信设备。");
				setTimeout(function(){
					$("#loginForm")[0].submit();
				},1000)
			}else if(ret.info == 'codeErr'){
				layer.msg("验证码有误");
			}else if(ret.info == 'timeout'){
				layer.msg("验证码超时");
			}else if(ret.info == 'ok'){
				$("#loginForm")[0].submit();
			}else{
				layer.msg("验证失败");
			}
		}, 'json');
	});

}

(function ($) {
	$.getUrlParam = function (name) {
		var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
		var r = window.location.search.substr(1).match(reg);
		if (r != null) return unescape(r[2]);
		return null;
	}
})(jQuery);
