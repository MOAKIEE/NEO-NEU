/**
 * Created by jwu on 2024/5/21
 * E-mail:01313101@wisedu.com
 *
 * Description:教务加敏脱敏组件
 */

var JW_SECURITY = {};
(function (JW_SECURITY) {
    var formCheckAttr = ["checkSize", "checkType", "checkExp"];
    /**
     * 当开启脱敏时需要对部分字段增加脱敏类型
     * 脱敏表单字段类型为 custom-desensitize 类型
     */
    JW_SECURITY.initDesensitizeType = function (dataModel) {
        var currentDesensitizeArray = []; //当前正在展示的脱敏字段集
        if (dataModel && _JW_INIT_CONFIG.OPEN_DESENSITIZE) {
            for (var i = 0; i < dataModel.length; i++) {
                if (dataModel[i].sensitive) {
                    dataModel[i]["xtype"] = "custom-desensitize";
                    if ((dataModel[i]['readonly'] && dataModel[i]['form.readonly'] === undefined) || dataModel[i]['form.readonly']) {
                        dataModel[i]['readonly'] = false;
                        dataModel[i]['form.readonly'] = false;
                        dataModel[i].needDisable = true;  //先取消只读，渲染完成后再disable
                    }
                    if (_JW_INIT_CONFIG.NEED_DESENSITIZE) {//需要脱敏
                        //去除字段校验并记录
                        for (var j = 0; j < formCheckAttr.length; j++) {
                            var tmp = dataModel[i][formCheckAttr[j]];
                            if (tmp !== null && tmp !== undefined) {
                                delete dataModel[i][formCheckAttr[j]];
                                dataModel[i][formCheckAttr[j] + "-desensitize"] = tmp;
                            }
                        }
                    } else {//数据库加密后会变长
                        dataModel[i].dataSize = Math.floor(dataModel[0].dataSize / 3);
                    }
                    currentDesensitizeArray.push(dataModel[i]);
                }
            }
        }
        //返回当前表单中的脱敏字段集合，方便只读形式增加脱敏展示功能
        return currentDesensitizeArray;
    };
    JW_SECURITY.getJwFormCheckAttr = function () {
        return ["checkSize", "checkType", "checkExp"];
    };
    JW_SECURITY.getCurMenu = function () {
        var hashHref = location.hash;
        if (hashHref.startsWith('#/')) {
            hashHref = hashHref.substring(2);
        }
        return hashHref.split('?')[0];
    };
    /**
     * 当前需要脱敏的字段增加脱敏展示(只读表单)
     */
    JW_SECURITY.initViewDesensitize = function (desensitizeArr, container) {
        if (!_JW_INIT_CONFIG.NEED_DESENSITIZE) {
            return;
        }
        for (var i = 0; i < desensitizeArr.length; i++) {
            var name = desensitizeArr[i].name;
            var $obj = $(container).find('[data-name="' + name + '"]');
            $obj.addClass('custom-desensitize-p-width');
            var afterHtml = '<div class="custom-desensitize-right" style="margin-top: -32px;">';
            $obj.after(afterHtml);
            $obj.closest('[emap-role="input-wrap"]').addClass('bh-form-static');
            JW_SECURITY.initDesensitizeField($obj, $obj.siblings(".custom-desensitize-right"), false);
        }
    };

    /**
     * 非只读表单，但脱敏字段模型配置只读，此时表单渲染完成，将该字段disable
     */
    JW_SECURITY.initDisableDesensitize = function (desensitizeArr, container) {
        for (var i = 0; i < desensitizeArr.length; i++) {
            if (desensitizeArr[i].needDisable) {
                $(container).emapForm('disableItem', [desensitizeArr[i].name]);
            }
        }
    };
    /**
     * 初始化表单custom-desensitize类型
     * @Author   wadefl
     * @DateTime 2023-11-08T16:36:16+0800
     * @param customDesEdit
     * @param desEditCallback
     * @return   {[type]}                 [description]
     */
    JW_SECURITY.initDesensitizeForm = function (customDesEdit, desEditCallback) {
        if (!_JW_INIT_CONFIG.OPEN_DESENSITIZE) {
            return;
        }
        if (WIS_EMAP_INPUT.component['custom-desensitize']) {
            delete WIS_EMAP_INPUT.component['custom-desensitize'];
        }
        WIS_EMAP_INPUT.extend({
            xtype: 'custom-desensitize',
            init: function (ele, params) {
                $(ele).css('position', 'relative');
                if (_JW_INIT_CONFIG.NEED_DESENSITIZE) {
                    var name = $(ele).attr('data-name');
                    var html = '<input class="bh-form-control custom-desensitize-input" style="font-size: 0;" >';
                    $(ele).append(html);
                    //WIS_EMAP_INPUT.component['text'].init(ele,params);
                    html = '<div data-desensitive-name="' + name + '" class="bh-form-static bh-ph-4 custom-desensitize-line">'
                        + '<div class="custom-desensitize-p-width bh-str-cut" data-col-name="' + name + '"></div>'
                        + '<div class="custom-desensitize-right" >'
                        + '</div>';
                    +'</div>';
                    $(ele).append(html);
                    JW_SECURITY.initDesensitizeField($(ele), $(ele).find(".custom-desensitize-right"), true, customDesEdit, desEditCallback);
                } else {
                    var html = '<input class="bh-form-control jqx-widget-content jqx-input jqx-widget jqx-rc-all custom-desensitize-input" type="text" style="width: 100%;">'
                        + '<i class="iconfont icon-edit bh-table-form-icon custom-desensitize-editicon"></i>';
                    $(ele).append(html);
                    WIS_EMAP_INPUT.component['text'].init(ele, params);
                }
            },
            setValue: function (ele, name, val, root) {
                var nameDisplay = name + '_DISPLAY';
                var $form = $(ele).closest('[emap-role="form"]');
                if (_JW_INIT_CONFIG.NEED_DESENSITIZE) {//需要脱敏
                    if (val[name] && !val[nameDisplay]) {
                        var meta = $form.emapForm('getModel').filter(function (item) {
                            return item.name === name;
                        });
                        var params = {
                            value: val[name],
                            sensitive: meta[0]["sensitive"]
                        };

                        $.jwAjax({
                            url: '/sys/jwcommon/desens/secondAuth/autoGetFormData.do',
                            data: {
                                data: JSON.stringify(params)
                            },
                            successMsg: '',
                            async: false,
                            success: function (resp) {
                                var data = resp;
                                if ($(ele).find('.desensitize-open').is('.bh-hide')) {//打开状态下
                                    val[name] = data.value || '';
                                    //认证过期时，展示脱敏数据
                                    val[nameDisplay] = (data.isAuth ? data.displayValue : data.desensitive) || '';
                                    if (!(data.isAuth)) {//修改按钮状态
                                        $(ele).find('.desensitize-close').addClass('bh-hide');
                                        $(ele).find('.desensitize-open').removeClass('bh-hide');
                                    }
                                } else {
                                    val[name] = data.value || '';
                                    val[nameDisplay] = data.desensitive || '';
                                }
                            }
                        });
                    }
                    $(ele).find('input').val((val[name] !== null && val[name] !== undefined) ? val[name] : "");
                    $(ele).find('[data-desensitive-name="' + name + '"] [data-col-name="' + name + '"]').text(val[nameDisplay]);
                    $(ele).find('[data-desensitive-name="' + name + '"] [data-col-name="' + name + '"]').attr('title', val[nameDisplay]);
                } else {
                    var value = val[nameDisplay] || val[name];
                    $(ele).find('input').val((value !== null && value !== undefined) ? value : "");
                }
                $(ele).find('input').trigger('change');
            },
            getValue: function (ele, formData) {
                if (_JW_INIT_CONFIG.NEED_DESENSITIZE) {//需要脱敏
                    var name = $(ele).data('name');
                    formData[name + "_DISPLAY"] = $(ele).find('[data-desensitive-name="' + name + '"] [data-col-name="' + name + '"]').text() || "";
                }
                return $(ele).find('input').val();
            },
            disable: function (ele) {
                ele.find('input').attr('disabled', true);
                ele.find('.custom-desensitize-icon-edit').addClass('bh-hide');
            },
            enable: function (ele) {
                ele.find('input').attr('disabled', false);
                ele.find('.custom-desensitize-icon-edit').removeClass('bh-hide');
            }
        });
    };
    JW_SECURITY.initDesensitizeField = function ($ele, $container, needEdit, customDesEdit, desEditCallback) {
        var name = $ele.attr('data-name');
        JW_SECURITY.initDesensitize($container, {
            name: name,
            needEdit: !!needEdit,
            getValue: function ($element, params) {
                var $form = $ele.closest('[emap-role="form"]');
                return $form.emapForm("getValue")[name];
            },
            getModel: function ($element, params) {
                var $form = $ele.closest('[emap-role="form"]');
                var meta = $form.emapForm('getModel').filter(function (item) {
                    return item.name === name;
                });
                meta = JSON.parse(JSON.stringify(meta));
                //还原字段校验
                for (var i = 0; i < formCheckAttr.length; i++) {
                    var tmp = meta[0][formCheckAttr[i] + "-desensitize"];
                    if (tmp !== null && tmp !== undefined) {
                        delete meta[0][formCheckAttr[i] + "-desensitize"];
                        meta[0][formCheckAttr[i]] = tmp;
                    }
                }
                return meta;
            },
            getViewed: function (event, $element, params) {
                var $form = $ele.closest('[emap-role="form"]');
                var desUserKey = ($form.attr('des-user-key') || '').split(',');
                var userIdKeys = [desUserKey[0], "XSBH", "ZGH"];
                var userKeys = [desUserKey[1], "XH", "ZGH"];
                var userNameKeys = [desUserKey[2], "XM"];
                var formData = $form.emapForm("getValue");
                return [JW_SECURITY.getViewedField(formData, userIdKeys), JW_SECURITY.getViewedField(formData, userKeys), JW_SECURITY.getViewedField(formData, userNameKeys)];
            },
            callback: function (result, $element, params) {
                var $form = $ele.closest('[emap-role="form"]');
                var formData = {};
                formData[name] = result.value;
                formData[name + "_DISPLAY"] = result.displayValue;
                $form.emapForm("setValue", formData);
                if (result.isEdit) {
                    desEditCallback && desEditCallback(name, formData);
                }
            },
            customEdit: customDesEdit
        });
    };
    JW_SECURITY.getViewedField = function (formData, keys) {
        for (var i = 0; i < keys.length; i++) {
            if (keys[i] && formData[keys[i]] !== undefined && formData[keys[i]] !== null) {
                return formData[keys[i]];
            }
        }
        return "";
    };
    JW_SECURITY.initDesensitize = function (ele, params) {
        var $dom = $(ele);
        if (!_JW_INIT_CONFIG.NEED_DESENSITIZE) {
            for (var i = 0; i < $dom.length; i++) {
                params.initComplete && params.initComplete($($dom[i]), params, false);
            }
            return;
        }
        for (var i = 0; i < $dom.length; i++) {
            JW_SECURITY.initDesensitize0($($dom[i]), params);
        }

    };
    JW_SECURITY.initDesensitize0 = function ($ele, params) {
        params = params || {};
        var needEdit = !!params.needEdit;
        var html = '<div class="custom-desensitize-div">'
            + '<img alt="" class="custom-desensitize-image desensitize-open" title="显示敏感信息" src="' + contextPath + '/sys/jwcommon/public/images/eye-open.png"/>'
            + '<img alt="" class="custom-desensitize-image bh-hide desensitize-close" title="隐藏敏感信息" src="' + contextPath + '/sys/jwcommon/public/images/eye-close.png"/>'
            + (needEdit ? '<i class="iconfont icon-edit custom-desensitize-icon-edit"></i>' : "")
            + '</div>';
        $ele.html(html);
        //查看
        JW_SECURITY.initDesensitizeOpenEvent($ele, params);
        //隐藏
        JW_SECURITY.initDesensitizeCloseEvent($ele, params);
        if (needEdit) {
            //编辑
            JW_SECURITY.initDesensitizeEditEvent($ele, params);
        }
        params.initComplete && params.initComplete($ele, params, true);

    };
    JW_SECURITY.initDesensitizeOpenEvent = function ($ele, params) {
        $ele.find('.desensitize-open').off('click').on('click', function (event) {
            event.stopPropagation();
            var viewedParam = params.getViewed ? params.getViewed(event, $ele, params) : [];
            var formData = {
                value: params.getValue($ele, params),
                BCKRID: viewedParam[0],
                BCKR: viewedParam[1],
                BCKRXM: viewedParam[2]
            };
            if (!formData["value"]) { // 如果数据为空，只修改图标
                $ele.find('.desensitize-open').addClass('bh-hide');
                $ele.find('.desensitize-close').removeClass('bh-hide');
                params.callback && params.callback({value: "", displayValue: ""}, $ele, params);
                return;
            }
            JW_SECURITY.showTmCheckWin(function () {
                JW_SECURITY.queryUnDesensitiveInfo(formData, function (respData) {
                    $ele.find('.desensitize-open').addClass('bh-hide');
                    $ele.find('.desensitize-close').removeClass('bh-hide');
                    params.callback && params.callback(respData, $ele, params);
                });
            });
        });
    };
    JW_SECURITY.initDesensitizeCloseEvent = function ($ele, params) {
        //隐藏
        $ele.find('.desensitize-close').off('click').on('click', function (event) {
            event.stopPropagation();
            var dataModel = params.getModel ? params.getModel($ele, params) : [];
            var formData = {
                value: params.getValue($ele, params),
                sensitive: dataModel[0]["sensitive"]
            };
            if (!formData["value"]) {  // 如果数据为空，只修改图标
                $ele.find('.desensitize-close').addClass('bh-hide');
                $ele.find('.desensitize-open').removeClass('bh-hide');
                params.callback && params.callback({value: "", displayValue: ""}, params);
                return;
            }
            JW_SECURITY.queryDesensitiveInfo(formData, function (respData) {
                $ele.find('.desensitize-close').addClass('bh-hide');
                $ele.find('.desensitize-open').removeClass('bh-hide');
                params.callback && params.callback(respData, $ele, params);
            });
        });
    };
    JW_SECURITY.initDesensitizeEditEvent = function ($ele, params) {
        //编辑
        $ele.find('.custom-desensitize-icon-edit').off('click').on('click', function (event) {
            event.stopPropagation();
            JW_SECURITY.showTmCheckWin(function () {
                if (params.customEdit) {
                    params.customEdit(event, $ele, params);
                    return;
                }
                var dataModel = params.getModel ? params.getModel($ele, params) : [];
                if (!dataModel || dataModel.length !== 1) {
                    return;
                }
                var name = dataModel[0].name;
                dataModel[0].xtype = 'text';
                delete dataModel[0]['url'];
                delete dataModel[0]['groupName'];
                //长度/2
                dataModel[0].dataSize = Math.floor(dataModel[0].dataSize / 3);
                var btns = [{
                    text: "确定",
                    className: "bh-btn-primary",
                    callback: function () {
                        var $editForm = $('#add-desensitive-col');
                        var validate = $editForm.emapValidate('validate');
                        if (!validate) {
                            return false;
                        }
                        var data = $editForm.emapForm('getValue');
                        var formData = {
                            value: data[name],
                            sensitive: dataModel[0]["sensitive"]
                        };

                        $.jwAjax({
                            url: '/sys/jwcommon/desens/secondAuth/autoGetFormData.do',
                            data: {
                                data: JSON.stringify(formData)
                            },
                            successMsg: '',
                            async: false,
                            success: function (resp) {
                                var data = resp;
                                var result = {};
                                if ($ele.find('.desensitize-open').is('.bh-hide')) {//打开状态下
                                    result.value = data.value || '';
                                    //认证过期时，展示脱敏数据
                                    result.displayValue = (data.isAuth ? data.displayValue : data.desensitive) || '';
                                    if (!(data.isAuth)) {//修改按钮状态
                                        $ele.find('.desensitize-close').addClass('bh-hide');
                                        $ele.find('.desensitize-open').removeClass('bh-hide');
                                    }
                                } else {
                                    result.value = data.value || '';
                                    result.displayValue = data.desensitive || '';
                                }
                                result.isEdit = true;
                                params.callback && params.callback(result, $ele, params);
                            }
                        });
                    }
                }, {
                    text: "取消",
                    className: "bh-btn-default",
                    callback: function () {
                    }
                }];
                BH_UTILS.bhWindow('<div id="add-desensitive-col"></div>', '编辑', btns, {
                    height: '300px',
                    width: '400px'
                });
                $('#add-desensitive-col').emapForm({
                    data: dataModel,
                    model: 'v'
                });
            });
        });
    };
    //查询脱敏
    JW_SECURITY.queryDesensitiveInfo = function (formData, queryCallback) {
        $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/getMaskData.do',
            data: {
                data: JSON.stringify(formData)
            },
            successMsg: '',
            success: function (resp) {
                if (queryCallback) {
                    queryCallback(resp || {});
                }
            },
            error: function (resp) {
                $.bhTip({content: resp.msg, state: 'warning'});
            }
        });
    };

    //查询显示
    JW_SECURITY.queryUnDesensitiveInfo = function (formData, queryCallback) {
        formData.appName = WIS_CONFIG.APPNAME;
        formData.hash = JW_SECURITY.getCurMenu();

        $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/getOriginData.do',
            data: {
                data: JSON.stringify(formData)
            },
            successMsg: '',
            success: function (resp) {
                if (queryCallback) {
                    queryCallback(resp || {});
                }
            },
            error: function (resp) {
                $.bhTip({content: resp.msg, state: 'warning'});
            }
        });
    };

    //二次验证
    JW_SECURITY.showTmCheckWin = function (callback) {
        // 如果不需要脱敏二次认证
        if (!_JW_INIT_CONFIG.NEED_DESENSITIZE) {
            if (callback) {
                callback();
            }
            return false;
        }
        // 校验时间又没有过期
        window.SECOND_AUTH_INFO = $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/beginAuth.do',
            successMsg: '',
            async: false
        });

        if (SECOND_AUTH_INFO.isAuth) {  //没过期
            if (callback) {
                callback();
            }
            return false;
        }
        //弹窗
        JW_SECURITY.showDesenAuthWindow();

        // 绑定选择校验方式修改事件
        $('.control-checktm-select').off('change').on('change', function (event) {
            var checkType = $('.control-checktm-select').val();

            switch (checkType) {
                case 'ACCOUNT':
                    $('.checktm-zh-input[name="zh"]').removeClass('bh-hide');
                    $('.checktm-zh-input[name="zh"]').val(userId);
                    $('.checktm-dlmm').removeClass('bh-hide');
                    $('.checktm-yzm').addClass('bh-hide');
                    $('.checktm-rwm').addClass('bh-hide');
                    $('.custom-checktm-buttons').removeClass('bh-hide');
                    clearInterval(window['checktm-qrcode-interval']);
                    break;
                case 'SMS':
                case 'EMAIL':
                    var _v = 'SMS' === checkType ? SECOND_AUTH_INFO.SJH : SECOND_AUTH_INFO.DZXX;// TODO 参数？
                    $('.checktm-zh-input[name="zh"]').removeClass('bh-hide');
                    $('.checktm-zh-input[name="zh"]').val(_v || '');
                    $('.checktm-dlmm').addClass('bh-hide');
                    $('.checktm-yzm').removeClass('bh-hide');
                    $('.checktm-rwm').addClass('bh-hide');
                    $('.custom-checktm-buttons').removeClass('bh-hide');
                    clearInterval(window['checktm-qrcode-interval']);
                    break;
                case 'QRCODE':
                    $('.checktm-zh-input[name="zh"]').addClass('bh-hide');
                    $('.checktm-dlmm').addClass('bh-hide');
                    $('.checktm-yzm').addClass('bh-hide');
                    $('.checktm-rwm').removeClass('bh-hide');
                    $('.custom-checktm-buttons').addClass('bh-hide');
                    //获取二维码
                    JW_SECURITY.createDesenAuthQrCode(SECOND_AUTH_INFO.RZID, callback);
                    break;
            }
        });
        $('.control-checktm-select').change();

        // 发送验证码事件
        $('.checktm-zh-input .query-yzm-a').off('click').on('click', JW_SECURITY.sendDesenVerifyCode);
        //取消
        $('.custom-tm-alert-row').closest('.jqx-window').on('close', function () {
            callback = null;
            clearInterval(window['checktm-qrcode-interval']);
        });
        $('#custom-checktm-cancel').off().on('click', function () {
            BH_UTILS.bhWindow.close();
        });
        //确定
        $('#custom-checktm-confirm').off().on('click', function () {
            JW_SECURITY.submitDesenAuth(callback);
        });
    };

    //弹窗
    JW_SECURITY.showDesenAuthWindow = function () {
        var html = '';  //BEGIN
        html += '<div class="custom-tm-alert-row" >'
            + '<i class="iconfont icon-setstyle icon-info custom-tm-alert-size"></i>'
            + '<span class="custom-tm-alert-info">查看敏感信息，需进行身份验证</span>'
            + '</div>'
            + '<div class="custom-checktm-flex">'
            + '<select class="control-checktm-select" name="type">';
        // 获取认证方式
        var typeArr = [];

        typeArr = $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/getAuthType.do',
            successMsg: '',
            async: false
        });

        for (var i = 0; i < typeArr.length; i++) {
            var checkType = typeArr[i];

            switch (checkType) {
                case 'ACCOUNT':
                    html += '<option value="' + checkType + '">账号密码</option>';
                    break;
                case 'SMS':
                    html += '<option value="' + checkType + '">短信验证码</option>';
                    break;
                case 'EMAIL':
                    html += '<option value="' + checkType + '">邮箱验证码</option>';
                    break;
                case 'QRCODE':
                    html += '<option value="' + checkType + '">二维码</option>';
                    break;
            }
        }

        html += '</select>'
            + '<input type="text" class="bh-form-control checktm-zh-input custom-tm-input bh-pull-right" readonly name="zh">'
            + '</div>'
            + '<div class="custom-checktm-flex checktm-dlmm"><input autocomplete ="new-password" type="password" class="bh-form-control checktm-zh-input" name="dlmm" placeholder="请输入登录密码"></div>'
            + '<div class="custom-checktm-flex checktm-yzm checktm-zh-input"><input type="text" class="input-yzm-view" name="yzm" placeholder="请输入验证码"><a class="query-yzm-a">获取验证码<span class="yzm-timer bh-hide">(<span class="yzm-timer-second ">30</span>s)</span></a></div>'
            + '<div class="checktm-rwm"><div class="bh-l-inline" id="checktm-qrcode"></div></div>';

        html += '<div class="custom-checktm-buttons">'
            + '<button type="button" class="bh-btn bh-btn-default" id="custom-checktm-cancel">取消</button>'
            + '<button type="button" class="bh-btn bh-btn-primary" id="custom-checktm-confirm">确定</button>'
            + '</div>';  //END

        BH_UTILS.bhWindow(html, '验证身份信息', [], {height: '300px', width: '350px'});

        if (typeArr.length <= 1) {
            $('.control-checktm-select').addClass('bh-hide');  // 只有一种验证方式时，不显示下拉选择
        }
    };

    //获取二维码
    JW_SECURITY.createDesenAuthQrCode = function (rzid, callback) {
        $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/createAuthQrCode.do',
            data: {
                RZID: rzid
            },
            async: true,
            successMsg: '',
            success: function (resp) {
                var codeUrl = resp.url;
                $('#checktm-qrcode').empty();
                $('#checktm-qrcode').qrcode({width: 125, height: 125, text: codeUrl});
                clearInterval(window['checktm-qrcode-interval']);
                window['checktm-qrcode-interval'] = setInterval(function () {
                    $.jwAjax({
                        url: '/sys/jwcommon/secondAuth/checkAuth.do',
                        data: {},
                        successMsg: '',
                        success: function (resp) {
                            if (resp && resp.isAuth) {
                                clearInterval(window['checktm-qrcode-interval']);
                                BH_UTILS.bhWindow.close();
                                if (callback) {
                                    callback();
                                }
                            }
                        }
                    });
                }, 2000);
            },
            error: function () {
                $.bhTip({content: "获取二维码失败", state: 'warning'});
            }
        });
    };

    //获取验证码
    JW_SECURITY.sendDesenVerifyCode = function () {
        var checkType = $('.control-checktm-select').val();
        if (!checkType) {
            $.bhTip({content: "请选择校验方式", state: 'warning'});
            return;
        }
        var zh = $('.checktm-zh-input[name="zh"]').val();
        if (!zh) {
            $.bhTip({content: "请维护账号信息", state: 'warning'});
            return;
        }
        var _p = {
            APPNAME: WIS_CONFIG.APPNAME,
            APPID: WIS_CONFIG.APPID,
            SJH: checkType === 'SMS' ? zh : "",
            EMAIL: checkType === 'EMAIL' ? zh : "",
            checkType: checkType
        };

        $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/sendVerifyCode.do',
            data: _p,
            successMsg: '',
            success: function () {
                $.bhTip({content: "发送成功，请及时查收", state: 'success'});
                var interval = 60 - 1;
                var resend = "获取验证码";
                var seconds = "秒";
                var $obj = $('.checktm-zh-input .query-yzm-a');
                $obj.text(resend + "(" + interval + seconds + ")").data({
                    "count": interval,
                    "stop": false
                }).attr('style', 'pointer-events:none;color:#C0C0C0');
                //倒计时
                var timerId = setInterval(function () {
                    var count = $obj.data("count");
                    var timerId = $obj.data("timerId");
                    var stop = $obj.data("stop");
                    if (stop) {
                        clearInterval(timerId);
                        $obj.removeData("count").removeData("timerId").removeData("stop");
                        var value = "发送验证码";
                        $obj.text(value).attr('style', '');
                        $obj.attr('style', '');
                        return;
                    }
                    count--;
                    if (count <= 0) {
                        clearInterval(timerId);
                        $obj.removeData("count").removeData("timerId").removeData("stop");
                        $obj.text(resend).prop("disabled", false);
                        $obj.attr('style', '');
                    } else {
                        $obj.text(resend + "(" + count + seconds + ")").data("count", count);
                    }
                }, 1000);
                $obj.data("timerId", timerId);
            },
            error: function (resp) {
                $.bhTip({content: resp.msg, state: 'warning'});
            }
        });
    };

    //提交验证
    JW_SECURITY.submitDesenAuth = function (callback) {
        var checkType = $('.control-checktm-select').val();
        if (!checkType) {
            $.bhTip({content: "请选择校验方式", state: 'warning'});
            return false;
        }
        if ('ACCOUNT' === checkType) {
            JW_SECURITY.submitByPassword(callback);
        } else if ('SMS' === checkType || 'EMAIL' === checkType) {
            JW_SECURITY.submitByVerifyCode(checkType, callback);
        }
    };
    JW_SECURITY.submitByPassword = function (callback) {
        var password = $('.checktm-zh-input[name="dlmm"]').val();
        if (!password) {
            $.bhTip({content: "请输入登录密码", state: 'warning'});
            return false;
        }
        //验证身份
        JW_SECURITY.getCryptoPwd(password).then(function (encryptData) {
            var params = {
                // RZID: SECOND_AUTH_INFO.RZID,
                PASSWORD: encryptData
            };

            $.jwAjax({
                url: '/sys/jwcommon/desens/secondAuth/authByPassword.do',
                data: params,
                successMsg: '',
                success: function () {
                    $.bhTip({content: "验证成功！", state: 'success'});
                    BH_UTILS.bhWindow.close();
                    if (callback) {
                        callback();
                    }
                },
                error: function (resp) {
                    // $.bhTip({content: resp.msg, state: 'warning'});
                }
            });
        });
    };
    JW_SECURITY.submitByVerifyCode = function (checkType, callback) {
        var yzm = $('.checktm-yzm [name="yzm"]').val();
        if (!yzm) {
            $.bhTip({content: "请输入验证码", state: 'warning'});
            return false;
        }
        //验证身份
        var params = {
            RZID: SECOND_AUTH_INFO.RZID,
            checkType: checkType,
            code: yzm
        };

        $.jwAjax({
            url: '/sys/jwcommon/desens/secondAuth/doAuth.do',
            data: params,
            successMsg: '',
            success: function (resp) {
                $.bhTip({content: "验证成功！", state: 'success'});
                BH_UTILS.bhWindow.close();
                if (callback) {
                    callback();
                }
            },
            error: function (resp) {
                $.bhTip({content: resp.msg, state: 'warning'});
            }
        });
    };
    JW_SECURITY.getCryptoPwd = function (pwd) {
        var dfd = $.Deferred();
        return dfd.resolve(A.encrypt(pwd));
    };
    //脱敏未开启的情况下将脱敏字段display的值赋给字段，防止展示密文
    JW_SECURITY.notDesenDisplayToValue = function (formData, dataModel) {
        if (!_JW_INIT_CONFIG.OPEN_DESENSITIZE && formData) {
            for (var i = 0; i < dataModel.length; i++) {
                if (dataModel[i].sensitive) {
                    var name = dataModel[i].name;
                    var nameDisplay = name + '_DISPLAY';
                    if (formData[nameDisplay]) {
                        formData[name] = formData[nameDisplay];
                    }
                }
            }
        }
    };

    /**
     * 检查模型中是否包含脱敏字段
     * @param dataModel 模型
     * @returns {boolean} 是否包含脱敏字段
     */
    JW_SECURITY.checkHasTMField = function (dataModel) {
        var result = false;
        if (_JW_INIT_CONFIG.OPEN_DESENSITIZE) {
            for (var i = 0; i < dataModel.length; i++) {
                // 判断是否字段被隐藏，如被隐藏则 pass
                if (dataModel[i].hasOwnProperty('grid.hidden')) {
                    if (dataModel[i]['grid.hidden']) {
                        continue;
                    }
                } else if (dataModel[i].hasOwnProperty('hidden')) {
                    if (dataModel[i]['hidden']) {
                        continue;
                    }
                }

                if (dataModel[i].sensitive) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    };

    JW_SECURITY.showTMFieldDisplayConfigWindow = function (key, dataModel, onConfirm) {
        if (!key) {
            throw new Error("请配置 key");
        }

        if (!Array.isArray(dataModel)) {
            throw new Error("请传入数组格式的 dataModel");
        }

        var tmFields = [];
        for (var i = 0; i < dataModel.length; i++) {
            // 判断是否字段被隐藏，如被隐藏则 pass
            if (dataModel[i].hasOwnProperty('grid.hidden')) {
                if (dataModel[i]['grid.hidden']) {
                    continue;
                }
            } else if (dataModel[i].hasOwnProperty('hidden')) {
                if (dataModel[i]['hidden']) {
                    continue;
                }
            }
            if (dataModel[i].sensitive) {
                tmFields.push(dataModel[i]);
            }
        }

        // 列出所有脱敏字段，提供勾选
        var html = '<div id="tm-display-config-root"></div>';
        html += '<div style="position: absolute;bottom:24px;width: 100%;left: 0;float: right;padding: 0 24px;">'
            + '<button type="button" class="bh-btn bh-btn-default bh-pull-right" id="tm-display-config-cancel">取消</button>'
            + (tmFields.length > 0 ? '<button type="button" class="bh-btn bh-btn-primary bh-pull-right" id="tm-display-config-confirm">确定</button>' : '')
            + '</div>';

        var $dom = BH_UTILS.bhWindow(html, '脱敏字段显示配置', [], {
            height: '500px',
            width: '500px'
        });

        var vueTpl = '<div>' +
            '<el-alert type="info" show-icon :closable="false" title="勾选需显示明文的字段"></el-alert>' +
            '<el-checkbox :indeterminate="isIndeterminate" v-model="checkAll" @change="handleCheckAllChange" style="margin-top: 12px">全选</el-checkbox>' +
            '<div v-if="tmFields.length > 0" style="padding: 6px;margin-top: 6px;border: 1px solid #D8DCF0">' +
            '<el-checkbox-group v-model="checkList" @change="handleCheckedChange">' +
            '<el-checkbox v-for="item in tmFields" :key="item.name" :label="item.name" style="margin-top: 6px;margin-bottom: 6px">{{ item.caption }}</el-checkbox>' +
            '</el-checkbox-group>' +
            '</div>' +
            '<el-empty v-else :image-size="100" description="没有脱敏字段以供配置"></el-empty>' +
            '</div>';

        var instance = null;

        A.loadElementUI(function () {
            $("#tm-display-config-root").html(vueTpl);

            instance = new Vue({
                el: "#tm-display-config-root",
                name: "TMFieldDisplayConfigWindow",
                data: function () {
                    var lastDisplayConfig = JW_SECURITY.getTMFieldDisplayConfig(key) || [];

                    return {
                        tmFields: tmFields,
                        checkList: lastDisplayConfig,
                        isIndeterminate: lastDisplayConfig.length > 0 && lastDisplayConfig.length < tmFields.length,
                        checkAll: lastDisplayConfig.length === tmFields.length
                    };
                },
                methods: {
                    handleCheckAllChange: function (val) {
                        if (val) {
                            var temp = [];
                            this.tmFields.forEach(function (item) {
                                temp.push(item.name);
                            });
                            this.checkList = temp;
                        } else {
                            this.checkList = [];
                        }

                        this.isIndeterminate = false;
                    },
                    handleCheckedChange: function (value) {
                        var checkedCount = value.length;
                        this.checkAll = checkedCount === this.tmFields.length;
                        this.isIndeterminate = checkedCount > 0 && checkedCount < this.tmFields.length;
                    },
                    getResult: function () {
                        return this.checkList;
                    }
                }
            });
        });

        $('#tm-display-config-cancel').off().on('click', function () {
            BH_UTILS.bhWindow.close();
            $dom.jqxWindow('destroy');// 防止双层弹窗后，无法关闭底层弹窗
        });
        $('#tm-display-config-confirm').off().on('click', function () {
            var result = instance.getResult();
            if (result.length === 0) {
                // 记录勾选结果
                JW_SECURITY.setTMFieldDisplayConfig(key, result);
                BH_UTILS.bhWindow.close();
                onConfirm(result);
            } else {
                JW_SECURITY.showTmCheckWin(function () {
                    // 记录勾选结果
                    JW_SECURITY.setTMFieldDisplayConfig(key, result);
                    BH_UTILS.bhWindow.close();
                    $dom.jqxWindow('destroy');// 防止双层弹窗后，无法关闭底层弹窗
                    onConfirm(result);
                });
            }
        });
    };

    JW_SECURITY.setTMFieldDisplayConfig = function (key, data) {
        if (!key) {
            throw new Error("请配置 key");
        }

        var pageName = location.hash.replace(/\/|#/g, '');
        var id = pageName + "_" + key;

        if (!window["__tm_field_display_config"]) {
            window["__tm_field_display_config"] = {};
        }

        window["__tm_field_display_config"][id] = JSON.stringify(data);
    };

    JW_SECURITY.getTMFieldDisplayConfig = function (key) {
        if (!key) {
            throw new Error("请配置 key");
        }

        var pageName = location.hash.replace(/\/|#/g, '');
        var id = pageName + "_" + key;

        if (!window["__tm_field_display_config"]) {
            return null;
        }

        var result = window["__tm_field_display_config"][id];
        if (result) {
            result = JSON.parse(result);
        }

        return result;
    };

})(window.JW_SECURITY = JW_SECURITY);