(function (window) {
    var self;
    var utils;
    var ubaseUtils;
    var req = require;
    var DEBUG = _JW_COMMON_CONFIG.DEBUG || false;
    var contextPath = _JW_COMMON_CONFIG.contextPath;
    var absPageBase = contextPath + '/sys/jwcommon';

    var DesCrypto = {
        /**
         * 长度为8的倍数的字符串
         */
        key: 'iLj9NhY2Bj72G4bO',
        /**
         * 偏移量
         */
        iv: [56, 14, 6, 17, 28, 79, 43, 91],
        getKey: function () {
            return this.key;
        },
        getIv: function () {
            return this.iv;
        }
    };

    /**
     * 通用工具类
     */
    var COMMON = {
        _cache: {},
        _init: function () {
            self = this;
            req(['./config'], function (config) {
                window.APP_CONFIG = $.extend(true, {}, window.APP_CONFIG, config);
                req(['utils', 'ubaseUtils'], function () {
                    utils = req('utils');
                    ubaseUtils = req('ubaseUtils');
                    _afterInit();
                });
            }, function (error) {
                console && console.log(error);
            });
        },
        /**
         * 断言<br>
         * 断言某些对象或值是否符合规定，否则抛出异常。经常用于做变量检查
         */
        Assert: {
            /**
             * 断言对象是否不为{@code null} ，如果为{@code null} 抛出{@link Error} 异常
             *
             * <pre class="code">
             * A.Assert.notNull(clazz, "The class must not be null");
             * </pre>
             *
             * @param obj 被检查对象
             * @param errorMsgTemplate 错误消息模板，变量使用{}表示
             * @param params 参数
             * @return 被检查后的对象
             * @throws Error if the object is {@code null}
             */
            notNull: function (obj, errorMsgTemplate, params) {
                if (!obj) {
                    throw new Error(COMMON.formatStr(errorMsgTemplate, params));
                }
                return obj;
            },
            /**
             * 断言给定集合或对象非空
             *
             * <pre class="code">
             * Assert.notEmpty(collection, "Collection must have elements");
             * </pre>
             *
             * @param obj 被检查的集合
             * @param errorMsgTemplate 异常时的消息模板
             * @param params 参数列表
             * @return 非空集合
             * @throws Error if the {Array|Object} is {@code null} or has no elements
             */
            notEmpty: function (obj, errorMsgTemplate, params) {
                if (_.isEmpty(obj)) {
                    throw new Error(COMMON.formatStr(errorMsgTemplate, params));
                }
                return obj;
            }
        },
        /**
         * 格式化数字小数点
         * @param num
         * @param scale
         * @returns {*}
         */
        formatNumber: function (num, scale) {
            if (isNaN(num)) {
                return num;
            }
            return Number(num).toFixed(scale || 2);
        },
        /**
         * 获取数组中所有对象某个字段值的集合
         * @param arr
         * @param key
         * @returns {*}
         */
        getArrValues: function (arr, key) {
            if (_.isEmpty(arr)) {
                return;
            }
            var result = [];
            _.each(arr, function (data) {
                result.push(data[key]);
            });
            return result;
        },
        /**
         * 用require引入js
         * @param js
         * @param callback
         */
        requireJsArr: function (js, callback) {
            _getJs(js).done(function (jsArr) {
                if (_.isFunction(callback)) {
                    callback(jsArr);
                }
            });
        },
        /**
         * 用require引入单个js
         * @param js
         * @param callback
         */
        requireJs: function (js, callback) {
            _getJs(js).done(function (jsArr) {
                if (_.isFunction(callback)) {
                    callback(jsArr[0]);
                }
            });
        },
        unRequireJs: function (js, callback) {
            _removeJs(js).done(function () {
                if (_.isFunction(callback)) {
                    callback();
                }
            });
        },
        /**
         * 执行Ajax请求,封装常用返回格式
         * @param action
         * @param params
         * @returns {Promise}
         */
        executeAction: function (action, params) {
            var def = $.Deferred();
            action = suffixAction(action);
            utils.doAjax(action, params).done(function (resp) {
                if (resp.success || resp.code == '0') {
                    def.resolve(resp);
                } else {
                    self.err(resp.msg || '系统异常,请联系管理员。');
                    def.reject(resp);
                }
            }).fail(function (resp) {
                _failResp(resp);
                def.reject(resp);
            });
            return def.promise();
        },
        /**
         * 执行Ajax请求,简化版方法
         * @returns {*|Promise}
         */
        exec: function () {
            var ctx = this;
            return self.executeAction.apply(ctx, arguments);
        },
        /**
         * 执行Ajax请求
         * @param action
         * @param params
         * @returns
         */
        execute: function (action, params) {
            var def = $.Deferred();
            action = suffixAction(action);
            utils.doAjax(action, params).done(function (resp) {
                def.resolve(resp);
            }).fail(function (resp) {
                _failResp(resp);
                def.reject(resp);
            });
            return def.promise();
        },
        post: function (action, params, opt, hideLoading) {
            action = suffixAction(action);
            var deferred = $.Deferred();
            if (!hideLoading) {
                $('.app-ajax-loading').jqxLoader('open');
            }
            opt = $.extend({
                type: 'POST',
                url: action,
                traditional: true,
                data: params || {},
                dataType: 'json',
                success: function (resp) {
                    try {
                        if (opt.dataType == 'json' && typeof resp == 'string') {
                            resp = JSON.parse(resp);
                        }
                    } catch (e) {
                        console.error(e);
                    }
                    if (!hideLoading) {
                        $('.app-ajax-loading').jqxLoader('close');
                    }
                    deferred.resolve(resp);
                },
                error: function (resp) {
                    if (!hideLoading) {
                        $('.app-ajax-loading').jqxLoader('close');
                    }
                    deferred.reject(resp);
                }
            }, opt);
            $.ajax(opt);
            return deferred.promise();
        },
        /**
         * 执行同步Ajax请求，封装了常用的返回格式
         * @param action
         * @param params
         * @param type
         * @returns {*}
         */
        executeSyncAction: function (action, params, type) {
            var resp = this.executeSync(action, params, type);
            if (_.isObject(resp)) {
                if (resp.success || resp.code == '0') {
                    return resp;
                } else {
                    self.err(resp.msg || '系统异常,请联系管理员。');
                }
            } else {
                return resp;
            }
        },
        /**
         * 执行同步Ajax请求
         * @param action
         * @param params
         * @param type
         * @returns {*}
         */
        executeSync: function (action, params, type) {
            action = suffixAction(action);
            var result = null;
            $.ajax({
                url: action,
                type: 'POST',
                async: false,
                data: params,
                dataType: type,
                success: function (resp) {
                    result = resp;
                }
            }).fail(function (resp) {
                _failResp(resp);
            });
            return result;
        },
        getHrefHtml: function (text, attrs, clazz, params) {
            return self.getHtml('a', text, attrs, clazz, params);
        },
        getHtml: function (type, text, attrs, clazz, params) {
            return self.getDom(type, text, attrs, clazz, params).prop("outerHTML");
        },
        /**
         * 创建一个新的DOM
         * @param type
         * @param text
         * @param attrs
         * @param clazz
         * 形如：
         Q.getDom('a', '点击查看', {
                'data-action': '查看最近月份的工资',
                wid : 'wid'
            },'bh-hide');
         *
         * @returns {HTMLElement}
         */
        getDom: function (type, text, attrs, clazz) {
            var $temp;
            if (type == 'a') {
                $temp = $('<a href="javascript:void(0);" ></a>');
            } else {
                $temp = $('<' + type + '>' + '</' + type + '>');
            }
            text = text || '';
            $temp.html(text);
            if (!_.isEmpty(clazz)) {
                $temp.addClass(clazz);
            }
            if (!_.isEmpty(attrs)) {
                if (_.isArray(attrs)) {
                    //array
                    _.each(attrs, function (item) {
                        var vKey = item.key;
                        if (vKey.indexOf('@') > -1) {
                            vKey = vKey.replace('@', 'v-on:');
                        }
                        $temp.attr(vKey, item.value);
                    });
                } else {
                    //object
                    _.each(attrs, function (value, key) {
                        var vKey = key;
                        if (vKey.indexOf('@') > -1) {
                            vKey = vKey.replace('@', 'v-on:');
                        }
                        $temp.attr(vKey, value);
                    });
                }
            }
            return $temp;
        },
        /**
         * 将字符串转为date对象
         * @param dateStr
         * @returns {Date}
         */
        getDate: function (dateStr) {
            return new Date(Date.parse(dateStr.replace(/-/g, "/")));
        },
        /**
         * 初始化一个EMAP component
         * @param $ele
         * @param params
         * @param opt
         * @param type
         */
        cmpInit: function ($ele, params, opt, type) {
            params = params || {};
            opt = opt || {};
            type = type || $ele.attr('xtype');
            //创建input标签
            if (!type || type == 'text') {
                var $input = this.getDom('input', '', {
                    xtype: 'text',
                    'data-name': $ele.attr('data-name')
                }, 'bh-form-control');
                $ele.append($input);
                return;
            }
            //兼容标签的attr
            var attr;
            var attrObj;
            if ($ele.attr('data-x-attr')) {
                attr = $ele.attr('data-x-attr');
                attrObj = self.toJson(attr.replace(/'/g, '"'));
                $ele.data('attr', attrObj);
            }
            //兼容标签的JSONParam
            if ($ele.attr('data-x-jsonparam')) {
                attr = $ele.attr('data-x-jsonparam');
                attrObj = self.toJson(attr.replace(/'/g, '"'));
                $ele.data('jsonparam', attrObj);
            }
            WIS_EMAP_INPUT.component[type].init($ele, params, {}, opt);
        },
        /**
         * emap component设值
         * 如
         * Q.cmpSetValue($ele,'NAME',{
         *     NAME : 'AAA',
         *     NAME_DISPLAY : 'BBB'
         * })
         * @param $ele
         * @param name
         * @param formData
         * @param type
         */
        cmpSetValue: function ($ele, name, formData, type) {
            formData = $.extend({}, formData);
            type = type || $ele.attr('xtype');
            if (!type || type == 'text') {
                if ($ele[0].tagName != 'input') {
                    $ele = $ele.find('input[data-name="' + $ele.attr('data-name') + '"]');
                }
            }
            WIS_EMAP_INPUT.component[type].setValue($ele, name, formData);
        },
        /**
         * 获取emap component的值，返回一个对象
         * @param $ele
         * @param dataName
         * @param type
         */
        cmpGetValue: function ($ele, dataName, type) {
            var formData = {};
            dataName = dataName || $ele.attr('data-name');
            type = type || $ele.attr('xtype');
            if (!dataName) {
                console.error('not found attr : data-name');
                return;
            }
            if (!type || type == 'text') {
                if ($ele[0].tagName != 'input') {
                    $ele = $ele.find('input[data-name="' + dataName + '"]');
                }
            }
            var value = WIS_EMAP_INPUT.component[type].getValue($ele, formData);
            if (value !== undefined) {
                formData[dataName] = value;
            }
            return formData;
        },
        cmpDisable: function ($ele, type) {
            type = type || $ele.attr('xtype');
            if (!type) {
                console.error('not found attr : xtype');
                return;
            }
            WIS_EMAP_INPUT.disable($ele);
        },
        cmpEnable: function ($ele, type) {
            type = type || $ele.attr('xtype');
            if (!type) {
                console.error('not found attr : xtype');
                return;
            }
            WIS_EMAP_INPUT.enable($ele);
        },
        initSingleForm: function ($ele, model, data, opt) {
            $ele = $($ele);
            opt = opt || {};
            $ele.attr('data-y-role', 'single-form');
            if (!_.isArray(model)) {
                model = [model];
            }
            var item = model[0];
            self.initEmapForm($ele, model, null, $.extend({
                renderByGroup: false,
                mode: 't',
                itemOptions: _.set({}, item.name, {
                    width: $ele.width()
                })
            }, opt));
            $ele.find('.bh-form-label').remove();
            $ele.find('[emap-role="input-wrap"]').removeClass('bh-form-readonly-input bh-ph-8');
            $ele.find('[bh-form-role="bhForm"]').removeClass('bh-form-block bh-form-horizontal');
            var $item = $ele.find('[data-name="' + item.name + '"]');
            if (item.xtype == 'tree2') {
                $ele.find('[data-name="' + item.name + '"]').css('background-color', '#fff');
            }
            if (_.isFunction(opt.afterInit)) {
                opt.afterInit($item, $ele);
            }
            var width = $ele.closest('[bh-form-role="bhForm"]').width();
            $ele.width($ele.width() < width ? width : $ele.width());
            if (data) {
                $ele.emapForm('setValue', data);
            }
            $ele.trigger('resize');
        },
        findForm: function (eventOrTarget) {
            return this.findP(eventOrTarget, 'form');
        },
        findP: function (eventOrTarget, type) {
            var $target = eventOrTarget.target ? $(eventOrTarget.target) : $(eventOrTarget);
            if (type == 'form' || type == 'emapForm') {
                return $target.closest('[emap-role="form"]');
            } else {
                return $target.closest(type);
            }
        },
        getUserParams: function () {
            return utils.getUserParams();
        },
        /**
         * 传1个参数表示设置此对象，
         * 传2个参数表示设置键，值对
         * @param  {[type]} key   [description]
         * @param  {[type]} value [description]
         * @return {[type]}       [description]
         */
        putCache: function (key, value) {
            if (_.isObject(key)) {
                _.extend(this._cache, key);
            } else {
                this._cache[key] = value;
            }
        },
        /**
         * 获取缓存,可传键的数据和单个键
         * @param  {[type]} key [description]
         * @param get
         * @return {[type]}     [description]
         */
        getCache: function (key, get) {
            var cache;
            if (_.isArray(key)) {
                cache = {};
                _.each(key, function (k) {
                    cache[k] = self._cache[k];
                });
                return cache;
            }
            cache = this._cache[key];
            if (cache === undefined && _.isFunction(get)) {
                cache = get();
                this.putCache(key, cache);
            }
            return cache;
        },
        /**
         * 清除缓存,
         * 参数不传表示清除所有缓存，
         * 传字符串参数表示根据键清除，
         * 传数据表示键数据
         * @param  {[type]} key [description]
         * @return {[type]}     [description]
         */
        clearCache: function (key) {
            if (!key) {
                //清除所有缓存
                this._cache = {};
            } else if (_.isString(key)) {
                delete this._cache[key];
            } else if (_.isArray(key)) {
                _.each(key, function (k) {
                    delete self._cache[k];
                });
            }
        },
        /**
         * 数字转中文大写
         * [numberToChinese description]
         * @param  {[type]} num [description]
         * @return {[type]}     [description]
         */
        numberToChinese: function (num) {
            if (!/^\d*(\.\d*)?$/.test(num)) {
                return "Number is wrong!";
            }
            var AA = ["零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"];
            var BB = ["", "拾", "佰", "仟", "萬", "億", "点", ""];
            var a = ("" + num).replace(/(^0*)/g, "").split("."),
                k = 0,
                re = "";
            for (var i = a[0].length - 1; i >= 0; i--) {
                switch (k) {
                    case 0:
                        re = BB[7] + re;
                        break;
                    case 4:
                        if (!new RegExp("0{4}\\d{" + (a[0].length - i - 1) + "}$").test(a[0]))
                            re = BB[4] + re;
                        break;
                    case 8:
                        re = BB[5] + re;
                        BB[7] = BB[5];
                        k = 0;
                        break;
                }
                if (k % 4 == 2 && a[0].charAt(i + 2) != 0 && a[0].charAt(i + 1) == 0) re = AA[0] + re;
                if (a[0].charAt(i) != 0) re = AA[a[0].charAt(i)] + BB[k % 4] + re;
                k++;
            }
            if (a.length > 1) //加上小数部分(如果有小数部分)
            {
                re += BB[6];
                for (var j = 0; j < a[1].length; j++) re += AA[a[1].charAt(j)];
            }
            return re;
        },
        getTable: function () {
            var ctx = this;
            return self.getGrid.apply(ctx, arguments);
        },
        /**
         * 获取grid
         * @return {[type]} [description]
         */
        getGrid: function (eventOrTarget) {
            if (eventOrTarget) {
                eventOrTarget = eventOrTarget.target || eventOrTarget;
                return $(eventOrTarget).closest('[emap-role="datatable"]:visible');
            }
            return $($('[emap-role="datatable"]:visible')[0]);
        },
        getTableData: function (eventOrTarget, index) {
            var $table = self.getGrid(eventOrTarget);
            return $table.emapdatatable('getRowsData')[index];
        },
        /**
         * 获取表格选中数据
         * @param eventOrTarget
         * @returns Array
         */
        getTableCheckData: function (eventOrTarget) {
            var $table = self.getGrid(eventOrTarget);
            return $table.emapdatatable('checkedRecords');
        },
        getUbaseUtils: function () {
            return ubaseUtils;
        },
        resetFooter: function ($ele) {
            $ele = ($ele && $ele.length) || $('body').children('main').children('article');
            if ($ele.length) {
                this.getUbaseUtils().setContentMinHeight($ele);
            }
            self.resetPaperDialog();
        },
        getAttr: function (eventOrTargetOrElement, attrName) {
            if (!eventOrTargetOrElement) {
                return;
            }
            var $ele = $(eventOrTargetOrElement.target || eventOrTargetOrElement);
            return $ele.attr(attrName);
        },
        /**
         * 将对象转为GET查询参数
         * @param obj
         */
        toQueryParams: function (obj) {
            if (_.isEmpty(obj)) {
                return;
            }
            var queryParams = [];
            _.each(obj, function (value, key) {
                queryParams.push(key + '=' + value);
            });
            return queryParams.join('&');
        },
        //-- Emap组件相关操作
        /**
         * 初始化EMAP表单
         * @param  form 表单容器.可以是任意的DOM元素
         * @param  modelOrUrl  模型对象或者模型请求URL
         * @param  formAction EMAP模型动作ID,不传此参数,表示使用自定义模型.
         * @param  opts 可配置编辑属性readonly/显示样式mode等EMAP表单自有属性
         *                     opts.showFields为自定义显示列，数组格式,如['XSMC','LBDM']
         *                     opts.hideFields 自定义隐藏列.数组格式
         *                     opts.mode： 'S','v' 上下格式;'L','h' 左右格式;'T','t' 一行多列，默认3列
         */
        initEmapForm: function (form, modelOrUrl, formAction, opts) {
            opts = opts || {};
            var $form = $(form);
            var model = null;
            var meta = null;
            $form.emapForm('destroy');
            if (opts.data) {
                model = opts.data;
                delete opts.data;
                _.each(model, function (item) {
                    if (item.meta) {
                        meta = item.meta;
                        return false;
                    }
                });
            } else {
                if ($.type(modelOrUrl) == 'array') {
                    model = modelOrUrl;
                    _.each(model, function (item) {
                        if (item.meta) {
                            meta = item.meta;
                            return false;
                        }
                    });
                } else {
                    if (modelOrUrl && formAction) { //取EMAP标准模型
                        model = WIS_EMAP_SERV.getModel(modelOrUrl, formAction, 'form');
                    } else if (modelOrUrl) { //取自定义模型
                        model = this.executeSync(modelOrUrl);
                    }
                }
            }
            if (opts.showFields && opts.showFields.length > 0) {
                _.each(model, function (item) {
                    //当有此值的时候使用模型默认方法，而不是隐藏
                    if (!opts.useDefaultWhenShowFields) {
                        item.hidden = true;
                    }
                    _.each(opts.showFields, function (showField) {
                        if (showField == item.name) {
                            item.hidden = false;
                            return false;
                        }
                    });
                });
                delete opts.showFields;
            }
            if (opts.hideFields && opts.hideFields.length > 0) {
                _.each(model, function (item) {
                    _.each(opts.hideFields, function (hideField) {
                        if (hideField == item.name) {
                            item.hidden = true;
                            return false;
                        }
                    });
                });
                delete opts.hideFields;
            }
            //兼容大写格式
            if (opts.model === 'T') {
                opts.model = 't';
            }
            var options = $.extend(true, {
                root: contextPath,
                data: model,
                readonly: false,
                model: 'h',
                textareaEasyCheck: true,
                defaultOptions: {
                    tree: {
                        unblind: '/',
                        search: true
                    }
                }
            }, opts);
            $form.each(function () {
                if (meta) {
                    WIS_EMAP_SERV.modelName = meta.modelName;
                    WIS_EMAP_SERV.appName = meta.appName;
                    WIS_EMAP_SERV.url = meta.url;
                    WIS_EMAP_SERV.name = meta.name;
                }
                var $form = $(this);
                $form.emapForm(options);
            });
        },
        /**
         * 将临时文件保存为正式文件
         * @author mengbin
         * @date 2016年9月24日 上午8:33:54
         * @param $obj 调用对象
         * @param appName 默认取应用名称
         * @param opts 额外配置参数
         * @returns
         */
        saveTempFile: function ($obj, appName, opts) {
            var autoSave = $obj.data('autoSave');
            var def = $.Deferred();
            if (autoSave) {
                def.resolve({
                    success: true,
                    token: $obj.data('token')
                });
                return def.promise();
            }
            var bhVersion = APP_CONFIG.BH_VERSION || '1.1';
            var defaults = $.extend({}, opts);
            if (bhVersion === '1.1') {
                var resp = $obj.emapFileUpload('saveTempFile', defaults);
                def.resolve(resp);
                return def.promise();
            } else {
                return $obj.emapUpload('saveUpload', defaults);
            }
        },
        /**
         * 初始化上传组件
         * @param $obj 附件的宿主元素
         * @param opts 额外属性
         */
        initEmapUpload: function ($obj, opts) {
            $obj = $($obj);
            opts = opts || {};
            var bhVersion = APP_CONFIG.BH_VERSION || '1.1';
            var opts1 = (typeof EMAP_UPLOAD_CONFIG === 'undefined' ? {} : EMAP_UPLOAD_CONFIG);
            var defTypes = null;
            if (opts.BM && opts1[opts.BM]) {
                defTypes = opts1[opts.BM].type;
            }
            if (!opts.token) {
                opts.token = BH_UTILS.NewGuid();
            }
            $obj.data('autoSave', true);
            $obj.data('token', opts.token);
            var defaults = {
                contextPath: contextPath,
                storeId: 'file',
                type: defTypes,
                size: 10240,
                autoSave: true,
                uploadParam: {
                    authAppName: _JW_COMMON_CONFIG.appname,
                    authBeanName: opts['handler'],
                    customer: opts['handler'],
                    app: _JW_COMMON_CONFIG.appname
                }
            };
            if (bhVersion === '1.1') {
                return $obj.emapFileUpload($.extend(defaults, opts1, opts));
            } else {
                defaults.buttonType = 'block';
                return $obj.emapUpload($.extend(defaults, opts1, opts));
            }
        },
        /**
         * 初始化导出功能
         * @param grid 导出关联的表格对象,可以为空
         * @param opts 导出属性
         */
        initEmapExport: function (grid, opts) {
            var params = {};
            if (grid) {
                //判断表格是否有emap-role="datatable"属性
                if ($(grid).attr('emap-role') != 'datatable') {
                    grid = $(grid).find('.bh-grid-table');
                }
                // 表格列处理
                if (opts && !opts['colnames']) {
                    var names = [];
                    var _columns = $(grid).jqxDataTable('_columns');
                    if (_columns && _columns.length > 0) {
                        for (var i = 0; i < _columns.length; i++) {
                            if (_columns[i].hidden || !_columns[i].datafield || _columns[i].datafield == 'field_checkbox') {
                                continue;
                            }
                            var name = _columns[i].datafield.replace('_DISPLAY', '');
                            names.push(name);
                        }
                    }
                    if (names.length < 1) {
                        self.alert(self.i18N("pub.select_export_columns|请选择导出列"));
                        return;
                    }
                    params['colnames'] = names.join();
                }
                // 表格排序处理
                if (opts && !opts['order']) {
                    var orders = $(grid).emapdatatable('getSort');
                    if (orders) {
                        orders = orders.exp.replace('_DISPLAY', '');
                        params['*order'] = orders;
                        params['order'] = orders;
                    }
                }
            }
            opts = $.extend(params, opts);
            self.doExport(opts);
        },
        doExport: function (params) {
            var base = _JW_COMMON_CONFIG.basePath.replace('/' + _JW_COMMON_CONFIG.appname, '/emapcomponent');
            var exportUrl = base + '/imexport/export.do';
            return utils.doAjax(exportUrl, params, 'POST').done(function (ret) {
                if (ret && ret.attachment) {
                    location.href = base + '/file/getAttachmentFile/' + ret.attachment + '.do';
                }
            });
        },
        //--表格/卡片相关操作开始
        /**
         * 重新加载表格/卡片
         * @param grid 表格/卡片组件,不能为空
         * @param params 额外的参数
         * @param searcher 高级搜索组件
         * @param _first 是否跳转到首页 (内部使用)
         */
        reloadTable: function (grid, params, searcher, _first) {
            if (!grid) {
                grid = self.getGrid();
            }
            params = params || {};
            //没有传searcher则自动获取当前页面的高级搜索组件，除非params.noSearch = true
            if (!searcher && params.noSearch !== true) {
                var $tmp = $('[emap-role="advancedQuery"]:visible');
                if (_.isEmpty($tmp)) {
                    $tmp = $('.emap-FQ-container:visible');
                }
                searcher = _.isEmpty($tmp) ? undefined : $tmp;
            }
            if (_.isEmpty(grid) && !_.isEmpty(searcher)) {
                searcher.trigger('search');
                return;
            }
            var $grid = $(grid);
            var sp = {};
            var tableType = null;
            if ($grid.data('emapCard')) {
                tableType = 'card';
            }
            if (!tableType) {
                tableType = $grid.data('tableType') || 'table';
            }
            if (searcher) {
                var componentName = 'emapAdvancedQuery';
                if (searcher.hasClass('emap-FQ-container')) {
                    componentName = 'emapFilterQuery';
                }
                sp['querySetting'] = $(searcher)[componentName]('getValue') || '';
            }
            params = $.extend(sp, params);
            var callback = params.callback;
            if (callback) {
                delete params.callback;
            }
            if (tableType === 'grid') {
                $grid.emapGrid('reload', params, _first);
            } else if (tableType === 'table') {
                if (_first) {
                    $grid.emapdatatable('reloadFirstPage', params, callback);
                } else {
                    $grid.emapdatatable('reload', params);
                }
            } else if (tableType === 'card') {
                $grid.emapCard('reload', params, _first);
            } else if (tableType === 'editTable') {
                var $table = $grid.emapEditableDataTable('getEmapDataTable');
                if (_first) {
                    $table.emapdatatable('reloadFirstPage', params, callback);
                } else {
                    $table.emapdatatable('reload', params);
                }
            }
        },
        /**
         * 可编辑表格
         * @param  {[type]} grid   [jQuery选择器或jQuery对象]
         * @param  {[type]} opts   [初始化参数，需自行传关键参数pagePath,action,url]
         * @param  {[type]} params [传递给原生方法的params]
         * @return {[type]}        [description]
         */
        initEditableEmapGrid: function (grid, opts, params) {
            var $grid = $(grid);
            $grid.data('tableType', 'editTable');
            var disabledFields = opts.disabledFields;
            var editFields = opts.editFields;
            var options = $.extend(true, {
                newDataEmptyNum: 1,
                emapdatatable: {
                    isCellEditable: function (row, column, value, rowData, colModel) {
                        var field = colModel.name;
                        if (disabledFields && _.isArray(disabledFields)) { //如果有不可编辑字段，以不可编辑字段为准
                            return !_.includes(disabledFields, field);
                        } else if (editFields && _.isArray(editFields)) {
                            return _.includes(editFields, field);//如果有可编辑字段，以可编辑字段为准
                        } else if (colModel['grid.readonly'] !== undefined || colModel['readonly'] !== undefined) { //否则按照模型处理
                            return !(colModel['grid.readonly'] || colModel['readonly']);
                        } else {
                            return false;
                        }
                    }
                }
            }, opts);
            if ($grid.attr('data-no-custom-field') == '1') {
                options.emapdatatable.schema = false;
            }
            delete opts.disabledFields;
            delete opts.editFields;
            return $grid.emapEditableDataTable(options, params);
        },
        /**
         *
         * @param  {[type]}   grid    jquery对象或者选择器
         * @param  {[type]}   opts     表格生成参数，添加checkbox属性
         * @param  {[type]}   params   查询参数
         * @param  {Function} callback 回调方法
         */
        initEmapGrid: function (grid, opts, params, callback) {
            var $grid = $(grid);
            var columns = [];
            if (opts.checkbox == true) {
                delete opts.checkbox;
                var checkBoxColumns = {
                    colIndex: '0',
                    type: 'checkbox'
                };
                columns.push(checkBoxColumns);
            }
            if (opts && opts.customColumns && opts.customColumns.length > 0) {
                Array.prototype.push.apply(columns, opts.customColumns);
            }
            if (opts && opts.datamodel) {
                for (var i = 0; i < opts.datamodel.length; i++) {
                    var model = opts.datamodel[i];
                    if (model.meta) {
                        WIS_EMAP_SERV.modelName = model.meta.modelName;
                        WIS_EMAP_SERV.appName = model.meta.appName;
                        WIS_EMAP_SERV.url = model.meta.url;
                        WIS_EMAP_SERV.name = model.meta.name;
                        break;
                    }
                }
            }
            var gridOptions = $.extend({
                fastRender: true,
                sortable: true,
                columnsReorder: true,
                alwaysHide: ['WID', 'CJRQ', 'CZRQ', 'CZZ', 'CZZXM']
            }, opts, {
                customColumns: columns
            });
            //默认不配 高度定为最小十行，若配置numlinenum==0或者null 则无视高度
            if (opts.minLineNum === undefined) {
                gridOptions.minLineNum = 10;
            }
            if (opts.minLineNum === 0 || opts.minLineNum === null || opts.height === null) {
                gridOptions.minLineNum = null;
            }
            if ($grid.attr('data-no-custom-field') == '1') {
                gridOptions.schema = false;
            }
            var tableType = gridOptions.type;
            delete gridOptions.type;
            if (tableType === 'grid') {
                $grid.data('tableType', 'grid');
                var oldSwitch = gridOptions.gridAfterSwitch;
                if (oldSwitch) {
                    $grid.data('oldSwitch', oldSwitch);
                }
                var cookieType = $.cookie(_JW_COMMON_CONFIG.appname + '-' + $grid.selector);
                if (cookieType) {
                    gridOptions.type = cookieType;
                }
                gridOptions.gridAfterSwitch = function (type) {
                    var oldSwitch = $grid.data('oldSwitch');
                    if (oldSwitch) {
                        oldSwitch(type);
                    }
                    $.cookie(_JW_COMMON_CONFIG.appname + '-' + $grid.selector, type, {
                        expires: 30
                    });
                };
                $grid.emapGrid(gridOptions, params, callback);
                if (cookieType == 'card') {
                    $grid.emapGrid('renderTable', true);
                }
                if (oldSwitch) {
                    oldSwitch(cookieType || 'list');
                }
            } else if (tableType === 'card') {
                $grid.data('tableType', 'card');
                $grid.emapCard(gridOptions, params);
            } else {
                $grid.data('tableType', 'table');
                $grid.emapdatatable(gridOptions, params, callback);
            }
        },
        /**
         * 重新加载首页表格
         * @param grid
         * @param params
         * @param searcher
         */
        reloadTableToFirst: function (grid, params, searcher) {
            self.reloadTable(grid, params, searcher, true);
        },
        /**
         * 重置表格的额外参数属性
         * @param  {[type]} grid      jquery对象或者选择器
         * @param  {[type]} newParams 新传递的参数
         * @param  {[type]} reload  true执行刷新操作, function : 刷新后执行回调方法
         * @return {[type]}           [description]
         */
        resetEmapGridParams: function (grid, newParams, reload) {
            var $grid = $(grid);
            //设置initParams能直接修改params参数
            $grid.data('initParams', newParams);
            //使缓存的配置数据保持一致
            $grid.data('emapdatatable').settings.params = newParams;
            if (reload === true) {
                $grid.emapdatatable('reload', newParams);
            } else if (typeof (reload) === 'function') {
                //reload 传递为function则刷新此表格并执行回调
                $grid.emapdatatable('reload', newParams, reload);
            }
        },
        /**
         * 获取自定义列配置
         */
        setCustomColumns: function (opts) {
            var columns = [];
            if (opts.checkbox == true) {
                delete opts.checkbox;
                var checkBoxColumns = {
                    colIndex: '0',
                    type: 'checkbox',
                    pinned: true
                };
                columns.push(checkBoxColumns);
            }
            if (opts.opColRender) {
                var opColRender = opts.opColRender;
                var opColWidth = opts.opColWidth;
                delete opts.opColRender;
                delete opts.opColWidth;
                var opCol = {
                    colIndex: columns.length + '',
                    type: 'tpl',
                    column: {
                        text: self.i18N('pub.text.operation|操作'),
                        pinned: true,
                        width: opColWidth || '120px',
                        align: 'center',
                        cellsalign: 'center',
                        cellsRenderer: function (row, column, value, rowData) {
                            return opColRender(row, column, value, rowData);
                        }
                    }
                };
                columns.push(opCol);
            }
            opts.customColumns = opts.customColumns || [];
            $.extend(opts.customColumns, columns);
        },
        //--表格卡片相关操作结束

        //--高级搜索相关操作开始
        initAdvanceSearch: function (elem, opts, callback) {
            var params = {
                appName: _JW_COMMON_CONFIG.appname,
                compType: 'table',
                type: 'search'
            };
            if (opts && opts.compType != undefined) {
                params.compType = opts.compType;
            }
            var options = $.extend({
                type: 'search',
                params: {
                    params: self.base64_enc(self.toJsonStr(params))
                }
            }, opts);
            var searchMeta = WIS_EMAP_SERV.getModel(options.modelPagePath, options.actionName, options.type, options.params);
            //定制增加额外的查询字段
            if (opts.extendControls && opts.extendControls.length) {
                searchMeta.controls = searchMeta.controls.concat(opts.extendControls);
                delete opts.extendControls;
            }
            //定制隐藏查询字段
            if (opts.hiddenControls && opts.hiddenControls.length) {
                _.each(hiddenControls, function (hiddenControl) {
                    for (var j in searchMeta.controls) {
                        if (searchMeta.controls[j].name == hiddenControl) {
                            searchMeta.controls.splice(j, 1);
                        }
                    }
                });
            }
            if (typeof (searchMeta) !== 'undefined' && searchMeta.controls) {
                $.each(searchMeta.controls, function (i, item) {
                    if (item.xtype == 'date-ym' || item.xtype == 'date-local') {
                        item.format = item.xtype == 'date-ym' ? 'yyyy-MM' : '';
                        item.xtype = 'date-range';
                    } else if (item.xtype == 'select') {
                        item.xtype = 'multi-select2';
                    } else if (item.xtype == 'tree') {
                        item.xtype = 'multi-tree';
                        item.defaultBuilder = 'm_value_equal';
                    } else if (!item.xtype) {
                        item.defaultBuilder = item.defaultBuilder == 'equal' ? 'include' : item.defaultBuilder;
                    }
                    if (item.name == 'XMPY') {
                        item.defaultBuilder = 'upper_include';
                    }
                    if (item.JSONParam) {
                        // RES-315高级搜索节点都可选
                        try {
                            var p = self.toJson(item.JSONParam.replace(/'/g, '"'));
                            delete p.unselectableLevel;
                            delete p.parentNodeSelectable;
                            item.JSONParam = self.toJsonStr(p);
                        } catch (e) {
                        }
                    }
                });
            }
            var component = 'emapAdvancedQuery';
            if (opts.componentName === 'filter') {
                component = 'emapFilterQuery';
            }
            var $search = $(elem);
            $search[component]($.extend({
                contextPath: contextPath,
                schema: true,
                showTotalNum: true, // 实现清空搜索功能,需要在表格中配置:searchElement:$('#shbmzpjh-index-search') // 高级查询组件id
                data: searchMeta || [],
                unblind: '/',
                defaultItem: opts.defaultItem
            }, opts));
            //高级搜索,注册搜索事件
            $search.on('search', function (e, data, opts, dom) {
                if (callback) {
                    callback.call(this, e, data, opts, dom);
                }
            });
            if (component === 'emapFilterQuery' && !opts.showSearchKey) {
                $search.find('[data-field="_commonFilter"]').hide();
            }
        },
        //--高级搜索相关操作结束
        /**
         * 滚动到
         * @param $target
         * @param offset
         * @param $obj
         */
        scrollTo: function ($target, offset, $obj) {
            offset = offset || 0;
            var $root = $('html,body');
            $obj = $obj || $root;
            $target = $target || $root;
            $obj.animate({
                scrollTop: $target.offset().top + offset
            }, 'fast', 'linear', function () {
                $target.addClass('animated bounce');
                setTimeout(function () {
                    $target.removeClass('animated bounce');
                }, 1000);
            });
        },
        // 切换模块
        switchModule: function (module) {
            utils.switchModule(module);
        },
        /**
         * 弹出表单窗口
         * @param  {[Array|Object]} windowOpt [description]
         * @param  {[type]} formOpt   [description]
         * @param  {[type]} renderCb  [description]
         * @param  {[type]} okCb      [description]
         * @return {[type]}           [description]
         */
        openFormWindow: function (windowOpt, formOpt, renderCb, okCb) {
            //兼容处理form参数
            var modelOrUrl = formOpt['model'] || formOpt[0];
            var formAction = formOpt['action'] || formOpt[1];
            var fOpt = formOpt['opt'] || formOpt['options'] || formOpt[2];
            if (!modelOrUrl) {
                console.error('缺失必要参数');
                return;
            }
            var title = '默认标题';
            var id = '__dz_form_div';
            var winOpt = {};
            //兼容处理window参数
            if (_.isArray(windowOpt)) {
                title = windowOpt[0] || title;
                id = windowOpt[1] || id;
                winOpt = windowOpt[2] || winOpt;
            } else if (_.isObject(windowOpt)) {
                title = windowOpt['title'] || title;
                id = windowOpt['id'] || id;
                winOpt = windowOpt['opt'] || windowOpt['options'] || winOpt;
            } else {
                title = windowOpt;
            }
            var btns = winOpt['btns'];
            delete winOpt.btns;
            self.openWindow('<div id="' + id + '" ><div>', title, function () {
                var $f = $('#' + id);
                if (_.isFunction(okCb)) {
                    return okCb($f);
                }
            }, btns, winOpt);
            var $form = $('#' + id);
            if (_.isFunction(formOpt.initBefore)) {
                formOpt.initBefore($form, modelOrUrl, formAction, fOpt);
            }
            self.initEmapForm($form, modelOrUrl, formAction, fOpt);
            if (_.isFunction(renderCb)) {
                renderCb($form);
            }
        },
        /**
         * 定位到指定元素的warning
         * @param $ele
         * @param content
         * @param noBorder
         */
        warning: function ($ele, content, noBorder) {
            if (content) {
                $.bhTip({
                    content: content,
                    state: 'warning'
                });
            }
            if ($ele && $ele.length) {
                self.scrollTo($ele, -400);
                if (noBorder !== true) {
                    $ele.css('border', 'red 3px solid');
                    _.delay(function () {
                        $ele.css('border', 'none');
                    }, 1000);
                }
            }
        },
        alert: function (msg, fn, opts) {
            self.warn(msg, fn, opts);
        },
        /**
         * 错误弹出框
         * 兼容性：V1.1,V1.2
         * @param  {[type]}   msg  title 提示内容
         * @param  {Function} fn   回调方法
         * @param  {[type]}   opts 配置的参数,如果使用此配置，则认为调用utils.dialog
         */
        err: function (msg, fn, opts) {
            opts = $.extend(true, {
                title: self.i18N('pub.text.prompt|提示'),
                content: self.i18N(msg) || '',
                buttons: [{
                    text: self.i18N('pub.text.confirm|确认'),
                    className: 'bh-btn-danger'
                }],
                callback: fn
            }, opts);
            BH_UTILS.bhDialogDanger(opts);
        },
        /**
         * 警告提示框
         * 兼容性：V1.1,V1.2
         * @param  {[type]}   msg  title 提示内容
         * @param  {Function} fn   回调方法
         * @param  {[type]}   opts 配置的参数,如果使用此配置，则认为调用utils.dialog
         */
        warn: function (msg, fn, opts) {
            opts = $.extend(true, {
                title: self.i18N('pub.text.prompt|提示'),
                content: self.i18N(msg) || '',
                buttons: [{
                    text: self.i18N('pub.text.confirm|确认'),
                    className: 'bh-btn-warning'
                }],
                callback: fn
            }, opts);
            BH_UTILS.bhDialogWarning(opts);
        },
        msg: function (msg, fn, opts) {
            $.bhTip($.extend({
                content: self.i18N(msg) || '',
                state: 'success',
                hideWaitTime: 3000
            }, opts));
            if (fn) {
                fn.call(this);
            }
        },
        errMsg: function (msg, fn, opts) {
            $.bhTip($.extend({
                content: self.i18N(msg) || '',
                state: 'danger',
                hideWaitTime: 3000
            }, opts));
            if (fn) {
                fn.call(this);
            }
        },
        /**
         * 信息确认弹出框
         * 兼容性：V1.1,V1.2
         * @param  {[type]} msg  title 提示内容
         * @param  {[type]} yes  确定按扭回调
         * @param  {[type]} no   取消按钮回调
         * @param  {[type]} opts 配置参数
         */
        confirm: function (msg, yes, no, opts) {
            opts = $.extend(true, {
                title: self.i18N('pub.text.prompt|提示'),
                content: self.i18N(msg) || '',
                buttons: [{
                    text: self.i18N('pub.text.confirm|确认'),
                    className: 'bh-btn-warning',
                    callback: yes
                }, {
                    text: self.i18N('pub.text.cancel|取消'),
                    className: 'bh-btn-default',
                    callback: no
                }]
            }, opts);
            BH_UTILS.bhDialogWarning(opts);
        },
        /**
         * 信息确认弹出框,默认显示是和否
         * 兼容性：V1.1,V1.2
         * @param  {[type]} msg  title 提示内容
         * @param  {[type]} yes  确定按扭回调
         * @param  {[type]} no   取消按钮回调
         * @param  {[type]} opts 配置参数
         */
        yesNo: function (msg, yes, no, opts) {
            opts = $.extend(true, {
                title: self.i18N('pub.text.prompt|提示'),
                content: self.i18N(msg) || '',
                buttons: [{
                    text: self.i18N('pub.text.yes|是'),
                    className: 'bh-btn-warning',
                    callback: yes
                }, {
                    text: self.i18N('pub.text.no|否'),
                    className: 'bh-btn-default',
                    callback: no
                }]
            }, opts);
            BH_UTILS.bhDialogWarning(opts);
        },
        // 国际化方法调用
        // 第一个参数为国际化编码,必须要有, 后面参数为编码参数
        i18N: function () {
            if (!arguments || arguments.length < 1) {
                return '';
            }
            var args = [];
            for (var i = 0; i < arguments.length; i++) {
                args.push(arguments[i]);
            }
            return i18n(args.join(','), self.useI18N());
        },
        useI18N: function () {
            if (!APP_CONFIG) {
                return false;
            }
            return !!APP_CONFIG.USE_LANG;
        },
        fullscreen: function (elem) {
            var ret;
            if (elem.requestFullscreen) {
                ret = elem.requestFullscreen();
            } else if (elem.mozRequestFullScreen) {
                ret = elem.mozRequestFullScreen();
            } else if (elem.webkitRequestFullscreen) {
                ret = elem.webkitRequestFullscreen();
            } else if (elem.msRequestFullscreen) {
                ret = elem.msRequestFullscreen();
            } else if (typeof window.ActiveXObject != 'undefined') {
                self.warn("pub.not_support_fullscreen|此版本浏览器不支持全屏操作,请升级到最新版本");
            }
            return ret;
        },
        /**
         * 使用表单提交方式打开新窗口
         * 解决IE浏览器下,window.open()方法导致的
         * referer信息丢失问题
         */
        open: function (url) {
            var hashs = url.split('#');
            url = hashs[0];
            var hash = hashs.length > 1 ? hashs[1] : '';
            var html = '<form target="_blank" action="';
            var pos = url.indexOf('?');
            if (pos < 0) {
                if (hash) {
                    url += '#' + hash;
                }
                html += url + '"></form>';
            } else {
                var tmpUrl = url.substr(0, pos);
                if (hash) {
                    tmpUrl += '#' + hash;
                }
                html += tmpUrl + '">';
                var p = url.substr(pos + 1);
                if (p) {
                    var ts = p.split('&');
                    for (var i = 0; i < ts.length; i++) {
                        var t = ts[i].split('=');
                        if (t.length != 2) {
                            continue;
                        }
                        html += '<input type="hidden" name="' + t[0] + '" value="' + t[1] + '">';
                    }
                }
                html += '</form>';
            }
            var $f = $(html).hide();
            $('body').append($f);
            $f.submit();
            $f.remove();
        },
        /**
         * 前端部门封装的window组件
         * 兼容性：V1.1,V1.2
         * @param  {[type]}   title    标题
         * @param  {[type]}   content  内容
         * @param  {Function} callback 确定事件回调
         * @param  {[type]}   btns     按钮组
         * @param  {[type]}   options  配置项
         */
        openWindow: function (content, title, callback, btns, options) {
            options = $.extend({
                width: 800,
                height: window.innerHeight * 0.736
            }, options);
            return BH_UTILS.bhWindow(content, title, btns, options, callback);
        },
        /**
         * 纸质弹窗显示事件
         * @param  {[type]}   title   弹窗标题
         * @param  {[type]}   content 弹窗内容
         * @param  {Function} fn      DOM元素渲染结束执行的回调，相对于ready，不会产生卡顿感，但是渲染量过大时建议放在 ready事件中
         * @param  {[type]}   opts    close,closeBefore,open,openBefore,ready回调及其他配置
         */
        showPaperDialog: function (title, content, fn, opts) {
            $.bhPaperPileDialog.show($.extend({
                'title': self.i18N(title) || '',
                'content': content,
                'render': function (a1, a2) {
                    if (fn) {
                        fn.call(a2);
                    }
                }
            }, opts));
        },
        /**
         * 隐藏纸张对话框，会依次触发closeBefore()和close()
         */
        hidePaperDialog: function () {
            $.bhPaperPileDialog.hide();
        },
        /**
         * 判断给定元素是否在纸质弹窗中
         */
        isInPaperDialog: function (ele) {
            return $(ele).closest('.bh-paper-pile-dialog').length > 0;
        },

        /**
         * 重新计算纸质弹窗和footer位置的方法，对页面性能影响较大，慎用
         * millisecond   毫秒数，若传了此参数，则每50毫秒刷新一次页脚和FOOTER，到时终止
         */
        resetPaperDialog: function (millisecond) {
            var reset = function () {
                var hasPaperPipeDialog = $('.bh-paper-pile-dialog ').length > 0;
                if (hasPaperPipeDialog) {
                    $.bhPaperPileDialog.resetPageFooter();
                    $.bhPaperPileDialog.resetDialogFooter();
                }
                $.bhFooterAffix.resetPosition();
            };
            reset();
            if (millisecond) {
                for (var i = 0; i < millisecond;) {
                    setTimeout(function () {
                        reset();
                    }, i);
                    i += 50;
                }
            }
        },
        /**
         * 浮动弹窗层
         */
        popDialog: function ($target, html, opts) {
            var $pop = $('#popDialog');
            if ($pop.size() < 1) {
                $pop = $('<div></div>').attr('id', 'popDialog').prependTo($('body'));
            } else {
                $pop.jqxPopover('destroy');
            }
            $pop.html(html);
            $pop.jqxPopover($.extend({
                width: 500,
                selector: $target,
                position: 'left'
            }, opts)).jqxPopover('open');
        },
        /**
         * 等待进度条
         * @param $cont 容器对象
         */
        showLoading: function ($cont) {
            $cont = $cont || $("body");
            var $loading = $('#loading');
            if ($loading.size() < 1) {
                $loading = $('<div id="loading"></div>').prependTo($cont);
                $loading.jqxLoader({
                    width: 200,
                    height: 60,
                    imagePosition: 'top'
                });
            }
            $loading.jqxLoader('open');
        },
        /**
         * 隐藏进度条
         */
        hideLoading: function () {
            $('#loading').jqxLoader('close');
        },
        //-----编码类-----
        toJson: function (str) {
            if (!str) {
                return str;
            }
            if (_.isString(str)) {
                str = (str || '').replace(/'/g, '"');
                return JSON.parse(str);
            }
            console.warn('参数对象有误,非字符串');
            return str;
        },
        toJsonStr: function (obj) {
            return JSON.stringify(obj);
        },
        base64_enc: function (str) {
            return $.base64.encode(str, true);
        },
        base64_dec: function (str) {
            return $.base64.decode(str, true);
        },
        insertContent: function (ele, myValue, t) {
            var $t = $(ele)[0];
            if (document.selection) { // ie
                ele.focus();
                var sel = document.selection.createRange();
                sel.text = myValue;
                ele.focus();
                var l = $t.value.length;
                sel.moveStart('character', -l);
                var wee = sel.text.length;
                if (arguments.length == 3) {
                    sel.moveEnd("character", wee + t);
                    t <= 0 ? sel.moveStart("character", wee - 3 * t - myValue.length) : sel.moveStart("character", wee - t - myValue.length);
                    sel.select();
                }
            } else if ($t.selectionStart || $t.selectionStart == '0') {
                var startPos = $t.selectionStart;
                var endPos = $t.selectionEnd;
                var scrollTop = $t.scrollTop;
                $t.value = $t.value.substring(0, startPos) + myValue + $t.value.substring(endPos, $t.value.length);
                ele.focus();
                $t.selectionStart = startPos + myValue.length;
                $t.selectionEnd = startPos + myValue.length;
                $t.scrollTop = scrollTop;
                if (arguments.length == 3) {
                    $t.setSelectionRange(startPos - t,
                        $t.selectionEnd + t);
                    ele.focus();
                }
            } else {
                ele.value += myValue;
                ele.focus();
            }
            return $t.value;
        },
        encrypt: function (message, key) {
            var keyHex = CryptoJS.enc.Utf8.parse(key || DesCrypto.getKey());
            var ivString = byteToString(DesCrypto.getIv());
            var ivHex = CryptoJS.enc.Utf8.parse(ivString);
            return CryptoJS.DES.encrypt(message, keyHex, {
                iv: ivHex,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            }).ciphertext.toString(CryptoJS.enc.Base64);
        },
        decrypt: function (message, key) {
            var keyHex = CryptoJS.enc.Utf8.parse(key || DesCrypto.getKey());
            var ivString = byteToString(DesCrypto.getIv());
            var ivHex = CryptoJS.enc.Utf8.parse(ivString);
            return CryptoJS.DES.decrypt({
                ciphertext: CryptoJS.enc.Base64.parse(message)
            }, keyHex, {
                iv: ivHex,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            }).toString(CryptoJS.enc.Utf8);
        },
        /**
         * 是否有此授权按钮
         * @param authId
         * @returns {*|boolean}
         */
        hasButtonAuth: function (authId) {
            var cache = self.getCache('_button_auth__', function () {
                var tmp = {};
                _.each(require('configUtils').MODULES, function (module) {
                    if (module.buttons && module.buttons.length) {
                        _.each(module.buttons, function (btn) {
                            tmp[btn] = true;
                        });
                    }
                });
                return tmp;
            });
            return cache[authId] || false;
        },
        /**
         * emapform表单项显示隐藏控制
         * @param $form
         * @param options
         *      格式： {
         *          // 隐藏时是否清除表单值，默认不清除
         *          clearWhenHide: false
         *          //
         *          controls:[{
         *              // 触发change的项
         *              name : 'itemName',
         *              // 需要控制显示和隐藏的项，此变量为函数则调用函数动态获取
         *              items: ['item1','item2'],
         *              // 返回true，被控制的项显示，
         *              // 返回false，被控制的项隐藏
         *              show: function(value,name, event,$form){
         *                  return !!value;
         *              },
         *              // 每次改变都先隐藏的项，items为一个函数时用于控制隐藏其他项
         *              hideItems: []
         *          }]
         *      }
         * @param show 统一的显示和隐藏判断函数,如果controls中的项定义了判断函数，以项中定义的为准
         */
        formItemVisibleControl: function ($form, options, show) {
            var opt = $.extend({}, {
                clearWhenHide: false,
                show: show || function (value, name, event, $form) {
                    return !!value;
                }
            }, options);
            var controls = opt.controls;
            if (_.isEmpty(controls)) {
                return;
            }
            var clearWhenHide = opt.clearWhenHide;
            _.each(controls, function (control) {
                var name = control.name;
                var show = control.show || opt.show;
                // 每次改变都先隐藏的项
                var hideItems = control.hideItems;
                $form.on('change', '[data-name="' + name + '"]', function (event) {
                    if (!_.isEmpty(hideItems)) {
                        // 先隐藏所有隐藏项
                        $form.emapForm('hideItem', hideItems);
                    }
                    var items = control.items;
                    var value = $form.emapForm('getValue')[name];
                    var isShow = show(value, name, $form);
                    var opt = isShow ? 'showItem' : 'hideItem';
                    if (_.isFunction(items)) {
                        items = items(value, name, event, $form);
                    }
                    $form.emapForm(opt, items);
                    if (!isShow && clearWhenHide) {
                        // 隐藏时清空值
                        var clearValue = {};
                        _.each(items, function (item) {
                            clearValue[item] = '';
                        });
                        $form.emapForm('setValue', clearValue);
                    }
                });
            });
        },
        /** vue相关开始 **/

        /**
         * 加载Vue.js
         * @param cb vue加载完毕之后的回调
         */
        loadVue: function (cb) {
            var def = $.Deferred();
            if (self.getCache('isVue2Created')) {
                var vue2 = window.Vue;
                _.delay(function () {
                    def.resolve(vue2);
                }, 20);
                cb && cb(vue2);
                return def.promise();
            }
            var v = window.Vue;
            self.putCache('isVue2Created', true);
            def.resolve(v);
            cb && cb(v);
            return def.promise();
        },
        /**
         * 加载Vue3
         * @param cb
         * @returns {*}
         */
        loadVue3: function (cb) {
            var def = $.Deferred();
            if (self.getCache('isVue3Created')) {
                var vue3 = window.Vue3;
                _.delay(function () {
                    def.resolve(vue3);
                }, 20);
                cb && cb(vue3);
                return def.promise();
            }
            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            var vue3BasePath = absPageBase + '/public/script/common/lib/vue3/';
            var url = vue3BasePath + 'vue.global.js';
            var vue2 = window.Vue;
            if (vue2) {
                delete window.Vue;
            }
            this.requireJs(url, function (v) {
                define.amd = tmp;
                window.Vue3 = (v = v || window.Vue);
                // 创建app时
                var oldFn = window.Vue3.createApp;
                var createApp = function () {
                    var app = oldFn.apply(self, arguments);
                    app.config.globalProperties = {};
                    app.config.compilerOptions = {
                        onContextCreated: function (context) {
                            context.runtimeGlobalName = 'Vue3';
                        }
                    };
                    return app;
                };
                Vue3.createApp = createApp;
                if (vue2) {
                    window.Vue = vue2;
                    Vue.createApp = createApp;
                }
                self.putCache('isVue3Created', true);
                def.resolve(v);
                cb && cb(v);
            });
            return def.promise();
        },
        /**
         * 加载iView
         * @param cb iview加载完毕之后的回调
         * @param options 硬性重新加载
         */
        loadIView: function (cb, options) {
            var def = $.Deferred();
            options = options || {};
            var iviewBasePath = absPageBase + '/public/script/common/lib/iview/';
            _createLink(iviewBasePath + 'iview.css', null, options.force);
            // 加载自定义样式
            _createLink(iviewBasePath + 'iview-ext.css', null, options.force);
            // 后续根据主题切换样式
            // var theme = utils.getConfig('THEME') || 'blue';
            if (self.getCache('isIViewCreated')) {
                var ivw = window.iview;
                _.delay(function () {
                    def.resolve(ivw);
                }, 20);
                cb && cb(ivw);
                return def.promise();
            }

            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            self.loadVue(function () {
                self.requireJs(iviewBasePath + 'iview.min.js', function (iview) {
                    window.iview = (iview = iview || window.iview);
                    // 加载一些自己封装的组件
                    self.requireJs(iviewBasePath + 'iview-ext.js', function () {
                        self.putCache('isIViewCreated', true);
                        define.amd = tmp;
                        def.resolve(iview);
                        cb && cb(iview);
                    });
                });
            });
            return def.promise();
        },
        /**
         * 加载element-ui
         * @param cb
         * @param options
         */
        loadElementUI: function (cb, options) {
            var def = $.Deferred();
            options = options || {};
            var elementUIBasePath = absPageBase + '/public/script/common/lib/element-ui/';

            _createLink(elementUIBasePath + 'element-ui.css', null, options.force);
            // 加载自定义样式
            _createLink(elementUIBasePath + 'element-ui-ext.css', null, options.force);
            // 后续根据主题切换样式
            // var theme = utils.getConfig('THEME') || 'blue';
            if (self.getCache('isElementUICreated')) {
                var elm = window.ELEMENT;
                _.delay(function () {
                    def.resolve(elm);
                }, 20);
                cb && cb(elm);
                return def.promise();
            }
            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            self.loadVue(function () {
                self.requireJs(elementUIBasePath + 'element-ui.min.js', function (ELEMENT) {
                    window.ELEMENT = (ELEMENT = ELEMENT || window.ELEMENT);
                    // 加载一些自己封装的组件
                    self.requireJs(elementUIBasePath + 'element-ui-ext.js', function () {
                        self.putCache('isElementUICreated', true);
                        define.amd = tmp;
                        def.resolve(ELEMENT);
                        cb && cb(ELEMENT);
                    });
                });
            });
            return def.promise();
        },
        /**
         * 加载iView
         * @param cb
         * @param options
         */
        loadViewUIPlus: function (cb, options) {
            var def = $.Deferred();
            options = options || {};
            var viewUIPlusBasePath = absPageBase + '/public/script/common/lib/viewuiplus/';
            _createLink(viewUIPlusBasePath + 'viewuiplus.css', null, options.force);
            // 加载自定义样式
            _createLink(viewUIPlusBasePath + 'viewuiplus-ext.css', null, options.force);
            // 后续根据主题切换样式
            // var theme = utils.getConfig('THEME') || 'blue';
            if (self.getCache('isViewUIPlusCreated')) {
                var vup = window.ViewUIPlus;
                _.delay(function () {
                    def.resolve(vup);
                }, 20);
                cb && cb(vup);
                return def.promise();
            }
            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            self.loadVue3(function (Vue3) {
                var vue2 = window.Vue;
                if (vue2) {
                    // 已加载Vue2
                    window.Vue = Vue3;
                }
                self.requireJs(viewUIPlusBasePath + 'viewuiplus.min.js', function (ViewUIPlus) {
                    // 加载一些自己封装的组件
                    self.requireJs(viewUIPlusBasePath + 'viewuiplus-ext.js', function () {
                        ViewUIPlus = ViewUIPlus || window.ViewUIPlus;
                        window.ViewUIPlus = ViewUIPlus;
                        // 创建app时安装ViewUIPlus类库
                        var oldFn = Vue3.createApp;
                        var createApp = function () {
                            var app = oldFn.apply(self, arguments);
                            app.use(ViewUIPlus);
                            // 输出一个全局的lastApp方便查看所有组件名称
                            ViewUIPlus._lastApp = app;
                            ViewUIPlus._components = app._context.components;
                            return app;
                        };
                        Vue3.createApp = createApp;
                        if (vue2) {
                            vue2.createApp = createApp;
                            window.Vue = vue2;
                        }
                        self.putCache('isViewUIPlusCreated', true);
                        define.amd = tmp;
                        def.resolve(ViewUIPlus);
                        cb && cb(ViewUIPlus);
                    });
                });
            });
            return def.promise();
        },
        /**
         * 加载ELementPlus
         * @param cb
         * @param options
         */
        loadElementPlus: function (cb, options) {
            var def = $.Deferred();
            options = options || {};
            var elementPlusBasePath = absPageBase + '/public/script/common/lib/element-plus/';
            _createLink(elementPlusBasePath + 'element-plus.css', null, options.force);
            // 加载自定义样式
            _createLink(elementPlusBasePath + 'element-plus-ext.css', null, options.force);
            // 后续根据主题切换样式
            // var theme = utils.getConfig('THEME') || 'blue';
            if (self.getCache('isElementPlusCreated')) {
                var vup = window.ElementPlus;
                _.delay(function () {
                    def.resolve(vup);
                }, 20);
                cb && cb(vup);
                return def.promise();
            }
            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            self.loadVue3(function (Vue3) {
                var vue2 = window.Vue;
                if (vue2) {
                    // 已加载Vue2
                    window.Vue = Vue3;
                }
                self.requireJs([elementPlusBasePath + 'element-plus.min.js', elementPlusBasePath + 'element-plus-icons.js'], function (ElementPlus, ElementPlusIconsVue) {
                    ElementPlus = window.ElementPlus = (ElementPlus || window.ElementPlus);
                    // 加载一些自己封装的组件
                    self.requireJs([elementPlusBasePath + 'element-plus-ext.js', elementPlusBasePath + '/locale/zh-cn.js'], function (ElementPlusExt, ElementPlusLocaleZhCn) {
                        ElementPlusIconsVue = ElementPlusIconsVue || window.ElementPlusIconsVue;
                        ElementPlusLocaleZhCn = ElementPlusLocaleZhCn || window.ElementPlusLocaleZhCn;
                        var icons = {};
                        _.each(ElementPlusIconsVue, function (item, key) {
                            item.name = 'ElIcon' + key;
                            icons[item.name] = item;
                        });
                        window.ElementPlus = ElementPlus;
                        // 创建app时安装ElementPlus类库
                        var oldFn = Vue3.createApp;
                        var createApp = function () {
                            var app = oldFn.apply(self, arguments);
                            app.use(ElementPlus, {
                                locale: ElementPlusLocaleZhCn
                            });
                            // 图标库
                            _.each(icons, function (icon, iconName) {
                                app.component(iconName, icon);
                            });
                            // 输出一个全局的lastApp方便查看所有组件名称
                            ElementPlus._lastApp = app;
                            ElementPlus._components = app._context.components;
                            return app;
                        };
                        Vue3.createApp = createApp;
                        if (vue2) {
                            vue2.createApp = createApp;
                            window.Vue = vue2;
                        }
                        self.putCache('isElementPlusCreated', true);
                        define.amd = tmp;
                        def.resolve(ElementPlus);
                        cb && cb(ElementPlus);
                    });
                });
            });
            return def.promise();
        },
        /**
         * 加载vxe-table
         * @param cb
         * @param options
         * @return {*}
         */
        loadVxeTable: function (cb, options) {
            var def = $.Deferred();
            options = options || {};
            if (self.getCache('isVxeTableCreated')) {
                var vxeTable = window.VXETable;
                _.delay(function () {
                    def.resolve(vxeTable);
                }, 20);
                cb && cb(vxeTable);
                return def.promise();
            }
            var vxeTableBasePath = absPageBase + '/public/script/common/lib/vxe-table/';
            _createLink(vxeTableBasePath + 'vxe-table-style.css', null, options.force);
            // 加载自定义样式
            _createLink(vxeTableBasePath + 'vxe-table-ext.css', null, options.force);
            // 后续根据主题切换样式
            // var theme = utils.getConfig('THEME') || 'blue';
            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            self.loadVue3(function (Vue3) {
                var vue2 = window.Vue;
                if (vue2) {
                    // 已加载Vue2
                    window.Vue = Vue3;
                }
                // 先加载xe-utils
                self.requireJs(vxeTableBasePath + 'xe-utils.umd.min.js', function (XEUtils) {
                    XEUtils = XEUtils || window.XEUtils;
                    window.XEUtils = XEUtils;
                    // 再加载vxe-table
                    self.requireJs(vxeTableBasePath + 'vxe-table.umd.js', function (VXETable) {
                        VXETable = VXETable || window.VXETable;
                        window.VXETable = VXETable;
                        // 创建app时安装VXETable
                        var oldFn = Vue3.createApp;
                        var createApp = function () {
                            var app = oldFn.apply(self, arguments);
                            app.use(VXETable);
                            // 输出一个全局的lastApp方便查看所有组件名称
                            VXETable._lastApp = app;
                            VXETable._components = app._context.components;
                            return app;
                        };
                        Vue3.createApp = createApp;
                        if (vue2) {
                            vue2.createApp = createApp;
                            window.Vue = vue2;
                        }
                        self.putCache('isVxeTableCreated', true);
                        define.amd = tmp;
                        def.resolve(VXETable);
                        cb && cb(VXETable);
                    });
                });
            });
            return def.promise();
        },
        /**
         * 加载vxe-table-v3
         * @param cb
         * @param options
         * @return {*}
         */
        loadVxeTableV3: function (cb, options) {
            var def = $.Deferred();
            options = options || {};
            if (self.getCache('isVxeTableV3Created')) {
                var vxeTable = window.VXETable;
                _.delay(function () {
                    def.resolve(vxeTable);
                }, 20);
                cb && cb(vxeTable);
                return def.promise();
            }
            var vxeTableBasePath = absPageBase + '/public/script/common/lib/vxe-table-v3/';
            _createLink(vxeTableBasePath + 'vxe-table-v3-style.css', null, options.force);
            // 加载自定义样式
            _createLink(vxeTableBasePath + 'vxe-table-v3-ext.css', null, options.force);
            // 后续根据主题切换样式
            // var theme = utils.getConfig('THEME') || 'blue';
            //加载时,不使用amd
            var tmp = define.amd;
            delete define.amd;
            self.loadVue(function (Vue3) {
                var Vue = window.Vue;
                // 先加载xe-utils
                self.requireJs(vxeTableBasePath + 'vxe-table-v3-xe-utils.js', function (XEUtils) {
                    XEUtils = XEUtils || window.XEUtils;
                    window.XEUtils = XEUtils;
                    // 再加载vxe-table
                    self.requireJs(vxeTableBasePath + 'vxe-table-v3.js', function (VXETable) {
                        VXETable = VXETable || window.VXETable;
                        window.VXETable = VXETable;
                        Vue.use(VXETable);
                        self.putCache('isVxeTableV3Created', true);
                        define.amd = tmp;
                        def.resolve(VXETable);
                        cb && cb(VXETable);
                    });
                });
            });
            return def.promise();
        },
        html: function (value) {
            return jQuery.access(this, function (value) {
                var elem = this[0] || {},
                    i = 0,
                    l = this.length;

                if (value === undefined && elem.nodeType === 1) {
                    return elem.innerHTML;
                }

                // See if we can take a shortcut and just use innerHTML
                if (typeof value === "string" && !rnoInnerhtml.test(value) &&
                    !wrapMap[(rtagName.exec(value) || ["", ""])[1].toLowerCase()]) {

                    value = value.replace(rxhtmlTag, "<$1></$2>");

                    try {
                        for (; i < l; i++) {
                            elem = this[i] || {};

                            // Remove element nodes and prevent memory leaks
                            if (elem.nodeType === 1) {
                                jQuery.cleanData(getAll(elem, false));
                                elem.innerHTML = value;
                            }
                        }

                        elem = 0;

                        // If using innerHTML throws an exception, use the fallback method
                    } catch (e) {
                    }
                }

                if (elem) {
                    this.empty().append(value);
                }
            }, null, value, arguments.length);
        },
        /** vue相关结束 **/

        /**
         * 根据token调用emapcomponent接口下载单个文件
         * @param token
         */
        download: function (token) {
            var url = _JW_COMMON_CONFIG.contextPath + '/sys/emapcomponent/file/getFileByToken/{}.do'.replace('{}', token);
            window.open(url, '_blank');
        },
        /**
         * 文本格式化方式
         * 方式1:
         * Q.formatStr('我的{}是{}','ID','test');
         * 方式2
         * Q.formatStr('我的{description}是{name}',{description:'ID',name:'test'});
         * @returns str
         */
        formatStr: function (str, params) {
            if (arguments.length === 1) {
                return arguments[0];
            }
            if (Object.prototype.toString.call(arguments[1]) === '[object Object]') {
                // 对象类型
                var objArgs = arguments[1];
                _.each(objArgs, function (value, key) {
                    str = str.replace(new RegExp("\\{" + key + "\\}", "g"), value);
                });
            } else {
                // 数组
                var arrArgs = _.drop(Array.prototype.slice.call(arguments));
                _.each(arrArgs, function (value) {
                    str = str.replace('{}', value);
                });
            }
            return str;
        }
    };

    function i18n(content, useI18n) {
        if (useI18n) {
            return BH_UTILS.i18n(content);
        }
        var matchResult = content;
        var defaultText; // i18n 字典中无匹配结果时，显示的默认值
        var key = '';
        var params = [];
        var placeReg = /({\d+}?)/g;
        // 判断是否带key
        if (matchResult.indexOf('|') > -1) {
            if (matchResult.indexOf(',') > -1) {
                params = matchResult.split(',');
                matchResult = params.splice(0, 1)[0];
            }
            key = matchResult.split('|')[0];
            defaultText = matchResult.split('|')[1];
        } else {
            //判断key中是否有占位符
            var mats = matchResult.match(placeReg);
            if (mats) {
                mats = mats.map(function (item) {
                    return item.match(/\d+/)[0];
                });
                mats.sort(function (a, b) {
                    if (a && b) {
                        return a - b;
                    }
                });
                var words = matchResult.split(',');
                params = words.slice(-mats.length);
                key = words.slice(0, -mats.length).join(',');
            } else {
                key = matchResult;
            }
            defaultText = key;
        }
        var result; // 匹配结果
        if (useI18n && $.i18n) {
            result = $.i18n(key);
        } else {
            //result = key;
            result = defaultText;
        }
        if (!result) {
            return content;
        }
        //判断匹配到的值是否含有{}
        var checkTemp = result.match(placeReg);
        if (checkTemp !== null) {
            var temp = result;
            checkTemp.forEach(function (item) {
                var i = item.match(/\d+/)[0];
                temp = temp.replace(new RegExp('\\{' + i + '\\}', 'g'), params[i] || '');
            });
            result = temp;
        }
        /*if (result === key && defaultText !== '') {
            result = defaultText;
        }*/
        return result;
    }

    function byteToString(arr) {
        if (typeof arr === 'string') {
            return arr;
        }
        var str = '',
            _arr = arr;
        for (var i = 0; i < _arr.length; i++) {
            var one = _arr[i].toString(2),
                v = one.match(/^1+?(?=0)/);
            if (v && one.length == 8) {
                var bytesLength = v[0].length;
                var store = _arr[i].toString(2).slice(7 - bytesLength);
                for (var st = 1; st < bytesLength; st++) {
                    store += _arr[st + i].toString(2).slice(2);
                }
                str += String.fromCharCode(parseInt(store, 2));
                i += bytesLength - 1;
            } else {
                str += String.fromCharCode(_arr[i]);
            }
        }
        return str;
    }

    function _afterInit() {
        _.delay(function () {
            // 临时解决方案，待后期整改
            _afterCommonInit();
        }, 50);
    }

    function _afterCommonInit() {
        var cb = _JW_CONFIG.afterCommonInit;
        cb && cb(self);
    }

    function suffixAction(action) {
        var url = action;
        var queryParams = '';
        var paramIndex = _.indexOf(url, '?');
        if (paramIndex != -1) {
            queryParams = url.substring(paramIndex);
            url = url.substring(0, paramIndex);
        }
        if (!_.endsWith(url, '.do')) {
            url += '.do';
        }
        return url + queryParams;
    }

    function _failResp(resp) {
        var msg = resp.msg;
        if (msg) {
            self.err(msg);
        } else {
            switch (resp.status) {
                case 0:
                    self.err(self.i18N('pub.server.resp.code.0|服务器连接异常,请联系管理员'));
                    break;
                case 400:
                    self.err(self.i18N('pub.server.resp.code.400|参数有误!'));
                    break;
                case 401:
                    self.err(self.i18N('pub.server.resp.code.401|登录超时,请尝试刷新页面解决'));
                    break;
                case 403:
                    self.err(self.i18N('pub.server.resp.code.403|无权限访问!'));
                    break;
                case 404:
                    self.err(self.i18N('pub.server.resp.code.404|请求路径不存在(404 Not Found!)'));
                    break;
                default:
                    self.err(self.i18N('pub.server.resp.code.default|系统异常,请联系管理员。'));
                    break;
            }
        }
    }

    /**
     * 加载css文件
     * @param url
     * @param last 为true则在head的尾部加载（优先级高），否则加载的最头部（优先级最低）
     * @param force 硬性重新加载
     * @private
     */
    function _createLink(url, last, force) {
        var $link = $(document).find('link[href="{href}"]'.replace('{href}', url));
        if ($link.length && !force) {
            return;
        }
        var cnzz_s_tag = document.createElement('link');
        cnzz_s_tag.rel = 'stylesheet';
        cnzz_s_tag.type = 'text/css';
        cnzz_s_tag.href = url;
        var root_s = document.getElementsByTagName('head')[0];
        if (last) {
            root_s.appendChild(cnzz_s_tag);
        } else {
            var $cssNode = $(root_s).find('div.css-insertion-point');
            if (_.isEmpty($cssNode)) {
                $cssNode = $('<div class="css-insertion-point"></div>');
                $(root_s).prepend($cssNode[0]);
            }
            $cssNode.append(cnzz_s_tag);
        }
    }

    function _getJs(jsArr) {
        var def = $.Deferred();
        if (!_.isArray(jsArr)) {
            jsArr = [jsArr];
        }
        req(jsArr, function () {
            var args = Array.prototype.slice.call(arguments);
            /*if (args.length == 1) {
                args = args[0];
            }*/
            def.resolve(args);
        });
        return def.promise();
    }

    function _removeJs(jsArr) {
        var def = $.Deferred();
        if (_.isArray(jsArr)) {
            _.each(jsArr, function (item) {
                require.undef(item);
            });
        } else {
            require.undef(jsArr);
        }
        def.resolve();
        return def.promise();
    }

    function _getVersion(path) {
        return (DEBUG ? path : path + '.min') + '.js';
    }

    COMMON._init();
    var globalName = window._JW_CONFIG.COMMON_NAME;
    window[globalName] = COMMON;

})(window, document);
