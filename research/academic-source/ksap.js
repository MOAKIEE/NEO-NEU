define(function (require) {

    var utils = require('utils');
    var bs = require('../wdksBS');
    var ksaplc = require('../../../../../kwglapp/public/components/ksaplc/index');
    var wapks = require('../../../../../kwglapp/public/components/wapkslb/index');
    require('css!./ksap.css');
    Date.prototype.Format = function(fmt) { // author: meizz
        var o = {
            "M+" : this.getMonth() + 1, // 月份
            "d+" : this.getDate(), // 日
            "H+" : this.getHours(), // 小时
            "m+" : this.getMinutes(), // 分
            "s+" : this.getSeconds(), // 秒
            "q+" : Math.floor((this.getMonth() + 3) / 3), // 季度
            "S" : this.getMilliseconds() // 毫秒
        };
        if (/(y+)/.test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
        }
        for ( var k in o) {
            if (new RegExp("(" + k + ")").test(fmt)) {
                fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
            }
        }
        return fmt;
    };
    var viewConfig = {
        initialize: function ($element) {
            var indexView = utils.loadCompiledPage('ksapIndex', require);
            $element.html(indexView.render({}), true);
            this.initKsap();
            this.eventMap = {};
        },
        initKsap: function () {
            var self = this;
            $.jwAjax({
                url: bs.api.queryMyExamArrangeMent,
                data: {XNXQDM: CACHE_DQXNXQ.XNXQDM},
                successMsg: '',
                success: function (resp) {
                    self.initKscns(resp);
                    self.initKsaplc(resp.stuInfo, resp.arranged,resp.showkeys);
                    self.initWapks(resp.notArranged);
                }
            });
        },
        initKscns: function (resp) {
            var self = this;
            if (resp.stuInfo.SFCNSYD == '1' || resp.stuInfo.SFQYKSCNS != "1" || resp.stuInfo.SFYDKSCNS == "1") {
                return;
            }
            self.showKscns(self.formatKscnsDatas(resp.stuInfo), false);
            $('.jqx-window-close-button.jqx-icon-close').hide();
        },
        initKsaplc: function (stuInfo, arranged,showkeys) {
            var self = this;
            var params = {
                user: {
                    name: stuInfo.XM,
                    id: stuInfo.XH,
                    college: stuInfo.YXDM_DISPLAY,
                    major: stuInfo.ZYDM_DISPLAY
                },
                headerOperation: []
            };
            if (stuInfo.SFQYKSCNS == "1") {
                params.headerOperation.push({
                    text: '考试承诺书',
                    className: 'bh-btn-primary',
                    callback: function () {
                        self.showKscns(self.formatKscnsDatas(stuInfo), true);
                    }
                });
            }
            // var ksList = [];
            // $.each(arranged, function (index, item) {
            //     ksList.push(Object.assign({}, item,{
            //         // name: item.KCM,
            //         // id: item.KCH,
            //         status: item.KSZT, // 0未开始-1进行中-2已完成
            //         // time: item.KSSJMS,
            //         // place: item.JASMC,
            //         // seatNo: item.ZWH,
            //         // mainTeacher: item.SKJS,
            //         // KSSM: item.KSSM,
            //         // operations: [
            //         //     {
            //         //         text: '考试说明',
            //         //         className: 'bh-btn-primary',
            //         //         callback: function (item) {
            //         //             bs.showKssm(item);
            //         //         }
            //         //     }
            //         // ]
            //     }));
            // });
            params['list'] = arranged;
            params['showKeys']=showkeys;
            params['progress'] = {
                all: arranged.length,
                finished: stuInfo.YWCS
            };
            ksaplc('kwglksaplc', Object.assign({}, params, {
                titleFormatter: function (examItem) {
                    var title = examItem.KCM || '-';
                    var originKeysKCH = (showkeys || []).indexOf('KCH') >= 0 ? (examItem.KCH || '') : undefined;
                    var originKeysKXH = (showkeys || []).indexOf('KXH') >= 0 ? (examItem.KXH || '') : undefined;
                    var extraTitles = [originKeysKCH, originKeysKXH].filter(function (item) { return !!item; });
                    return title + (extraTitles.length ? ('('+extraTitles.join('-')+')') : '');
                },
                statusFormatter: function (examItem) {
                    // 0未开始-1进行中-2已完成
                    return examItem.KSZT;
                },
                detailsFormatter: function (examItem) {
                    var detailList = [];
                    (showkeys || []).forEach(function (itemKey) {
                        if (itemKey !== 'KCM' && itemKey !== 'KCH' && itemKey !== 'KXH') {
                            var tempObj = {
                                label: '',
                                value: examItem[itemKey] || '-'
                            };
                            switch (itemKey) {
                                case 'KSSJMS':
                                    tempObj.label = '考试时间';
                                    detailList.push(tempObj);
                                    break;
                                case 'JASMC':
                                    tempObj.label = '考试地点';
                                    detailList.push(tempObj);
                                    break;
                                case 'ZWH':
                                    tempObj.label = '考试座位号';
                                    detailList.push(tempObj);
                                    break;
                                case 'SKJS':
                                    tempObj.label = '上课教师';
                                    detailList.push(tempObj);
                                    break;
                                case 'XSS':
                                    tempObj.label = '考试人数';
                                    detailList.push(tempObj);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    return detailList;
                },
                operationsFormatter: function (examItem) {
                    return [
                        {
                            text: '考试说明',
                            className: 'bh-btn-primary',
                            callback: function (item) {
                                bs.showKssm(item);
                            }
                        }
                    ];
                }
            }));
        },
        applyBkHk:function(item){
        	//判断是否申请
            var sfysq = BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getAbsPath("/modules/wdks/cxsfysq.do"), 
            		{"XNXQDM" : item.XNXQDM, "XH" : userId,"JXBID":item.JXBID});
            if(sfysq.code=='0' && sfysq.datas.cxsfysq.rows.length>0){
            	$.bhTip({
                    content : '此考试已申请缓考！',
                    state : 'warning'
                });
                return false;
            }
            
            var cs_data = BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getAbsPath("/modules/wdks/cxcs.do"), {"CSDM" : "HKGL", "ZCSDM" : "HKCSSZ"});
            var hkcs = [];
            if(cs_data.code=='0' && cs_data.datas.cxcs.extParams.code=='1'){
                hkcs = cs_data.datas.cxcs.rows;
            }
            if (hkcs.length > 0 && hkcs[0].SFQY == 0) {
                $.bhTip({
                    content : '缓考申请未开启，无法申请！',
                    state : 'warning'
                });
                return false;
            }
            if (hkcs.length > 0 && hkcs[0].SFQY == 1) {
                var date = new Date().Format("yyyy-MM-dd HH:mm:ss");
                if (date < hkcs[0].CSZA || date > hkcs[0].CSZB) {
                    $.bhTip({
                        content : '不在缓考申请允许时间范围内，无法申请！',
                        state : 'warning'
                    });
                    return false;
                }
            }
            if(item.XNXQDM != hkcs[0].CSSM){
                $.bhTip({
                    content : '当前学年学期非缓考学年学期，无法申请！',
                    state : 'warning'
                });
                return false;
            }
            
            var ysqms = BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getAbsPath("/modules/wdks/cxysqhkkcs.do"),
            		{XNXQDM:item.XNXQDM ,XH:userId});
            if(ysqms.code=='0' && ysqms.datas.cxysqhkkcs.rows[0].YSQCOUNT >= parseInt(hkcs[0].BZ)){
                $.bhTip({
                    content : "缓考申请课程已达到申请上限("+hkcs[0].BZ+"门)，无法申请！",
                    state : 'warning'
                });
                return false;
            }
          
            utils.window({
                title : '新建补考缓考申请',
                width : '750px',
                height : '650px',
                content : '<div id="addHksq-form" style="width: 700px;"></div>',
                buttons : [ { // 选填
                    text : '确定',
                    className : 'bh-btn-primary',
                    callback : function() {
                    	 if (!$("#addHksq-form").emapValidate("validate")) {
                             return false;
                         }
                         var data = $("#addHksq-form").emapForm("getValue");
                         var zmcl = data.ZMCL;
                         if (hkcs[0].CSCSMRZ == 1) {
                             if (!zmcl) {
                                 $.bhTip({
                                     content : '请上传正确格式的证明材料！',
                                     state : 'warning'
                                 });
                                 return false;
                             }
                         }
                         var param = [];
                         var kssq = {};
                         kssq.XNXQDM = item.XNXQDM;
                         kssq.SFYGHK = data.SFYGHK;
                         if (!zmcl) {
                             kssq.ZMCL = "";
                         } else {
                             kssq.ZMCL = zmcl;
                         }
                         kssq.JXBID = item.JXBID;
                         kssq.SQYY = data.SQYY;
                         kssq.TSYYDM = 0;
                         kssq.HKYY = data.HKYY;
                         param.push(kssq);
                         $('#addHksq-form').emapForm("saveUploadSync").done(function() {
                             var data={
                                 "requestParamStr":JSON.stringify(param)
                             };
                             utils.doAjax(WIS_EMAP_SERV.getAbsPath("api/bk/xshksq/xshksq/addBkHksq.do"),data,'post').done(function(res){
                                 if(res.code == '0')
                                 {
                                     $.bhTip({
                                         content : res.msg,
                                         state : 'success'
                                     });
                                 }else if(res.code!='0' && res.msg){
                                     $.bhTip({content:res.msg, state:'danger'});
                                 }else{
                                     $.bhTip({ content: res.data ? res.data.msg : '操作失败', state: 'warning'});
                                     return false;
                                 }
                             });
                         });
                    }
                }, {
                    text : '取消',
                    className : 'bh-btn-default',
                    callback : function() {
                    }
                } ]
            });
            var datamodel = WIS_EMAP_SERV.getModel("modules/wdks.do", 'hksqforzj', 'form');
            if (hkcs[0].CSCSMRZ == 1) {
                $.each(datamodel,function(index,item){
                    if(item.name==="ZMCL"){
                        item["form.required"]=true;
                    }
                });
            }
            $('#addHksq-form').emapForm({
                root : '',
                data : datamodel,
                textareaEasyCheck : true,
                readonly : false,
                model : 'v'
            });
            $("#addHksq-form").emapForm("setValue", {XNXQDM:item.XNXQDM,KCM:item.KCM,SFYGHK:'0'});
            $('#addHksq-form').emapForm('disableItem', [ 'XSBH', 'XNXQDM', 'KCM' ]);
        
        
        },
        applyHk:function(item){
        	
        	//判断是否申请
            var sfysq = BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getAbsPath("/modules/wdks/cxsfysq.do"), 
            		{"XNXQDM" : item.XNXQDM, "XH" : userId,"JXBID":item.JXBID});
            if(sfysq.code=='0' && sfysq.datas.cxsfysq.rows.length>0){
            	$.bhTip({
                    content : '此考试已申请缓考！',
                    state : 'warning'
                });
                return false;
            }
            
            var cs_data = BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getAbsPath("/modules/wdks/cxcs.do"), {"CSDM" : "HKGL", "ZCSDM" : "HKCSSZ"});
            var hkcs = [];
            if(cs_data.code=='0' && cs_data.datas.cxcs.extParams.code=='1'){
                hkcs = cs_data.datas.cxcs.rows;
            }
            if (hkcs.length > 0 && hkcs[0].SFQY == 0) {
                $.bhTip({
                    content : '缓考申请未开启，无法申请！',
                    state : 'warning'
                });
                return false;
            }
            if (hkcs.length > 0 && hkcs[0].SFQY == 1) {
                var date = new Date().Format("yyyy-MM-dd HH:mm:ss");
                if (date < hkcs[0].CSZA || date > hkcs[0].CSZB) {
                    $.bhTip({
                        content : '不在缓考申请允许时间范围内，无法申请！',
                        state : 'warning'
                    });
                    return false;
                }
            }
            if(item.XNXQDM != hkcs[0].CSSM){
                $.bhTip({
                    content : '当前学年学期非缓考学年学期，无法申请！',
                    state : 'warning'
                });
                return false;
            }
            
            var ysqms = BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getAbsPath("/modules/wdks/cxysqhkkcs.do"),
            		{XNXQDM:item.XNXQDM ,XH:userId});
            if(ysqms.code=='0' && ysqms.datas.cxysqhkkcs.rows[0].YSQCOUNT >= parseInt(hkcs[0].BZ)){
                $.bhTip({
                    content : "缓考申请课程已达到申请上限("+hkcs[0].BZ+"门)，无法申请！",
                    state : 'warning'
                });
                return false;
            }
          
            utils.window({
                title : '新建缓考申请',
                width : '750px',
                height : '650px',
                content : '<div id="addHksq-form" style="width: 700px;"></div>',
                buttons : [ { // 选填
                    text : '确定',
                    className : 'bh-btn-primary',
                    callback : function() {
                    	 if (!$("#addHksq-form").emapValidate("validate")) {
                             return false;
                         }
                         var data = $("#addHksq-form").emapForm("getValue");
                         var zmcl = data.ZMCL;
                         if (hkcs[0].CSCSMRZ == 1) {
                             if (!zmcl) {
                                 $.bhTip({
                                     content : '请上传正确格式的证明材料！',
                                     state : 'warning'
                                 });
                                 return false;
                             }
                         }
                         var param = [];
                         var kssq = {};
                         kssq.XNXQDM = item.XNXQDM;
                         kssq.SFYGHK = data.SFYGHK;
                         if (!zmcl) {
                             kssq.ZMCL = "";
                         } else {
                             kssq.ZMCL = zmcl;
                         }
                         kssq.JXBID = item.JXBID;
                         kssq.SQYY = data.SQYY;
                         kssq.TSYYDM = 0;
                         kssq.HKYY = data.HKYY;
                         param.push(kssq);
                         $('#addHksq-form').emapForm("saveUploadSync").done(function() {
                             var data={
                                 "requestParamStr":JSON.stringify(param)
                             };
                             utils.doAjax(WIS_EMAP_SERV.getAbsPath("api/xshksq/xshksq/addHksq.do"),data,'post').done(function(res){
                                 if(res.code == '0')
                                 {
                                     $.bhTip({
                                         content : res.msg,
                                         state : 'success'
                                     });
                                 }else if(res.code!='0' && res.msg){
                                     $.bhTip({content:res.msg, state:'danger'});
                                 }else{
                                     $.bhTip({ content: res.data ? res.data.msg : '操作失败', state: 'warning'});
                                     return false;
                                 }
                             });
                         });
                    }
                }, {
                    text : '取消',
                    className : 'bh-btn-default',
                    callback : function() {
                    }
                } ]
            });
            var datamodel = WIS_EMAP_SERV.getModel("modules/wdks.do", 'hksqforzj', 'form');
            if (hkcs[0].CSCSMRZ == 1) {
                $.each(datamodel,function(index,item){
                    if(item.name==="ZMCL"){
                        item["form.required"]=true;
                    }
                });
            }
            $('#addHksq-form').emapForm({
                root : '',
                data : datamodel,
                textareaEasyCheck : true,
                readonly : false,
                model : 'v'
            });
            $("#addHksq-form").emapForm("setValue", {XNXQDM:item.XNXQDM,KCM:item.KCM,SFYGHK:'0'});
            $('#addHksq-form').emapForm('disableItem', [ 'XSBH', 'XNXQDM', 'KCM' ]);
        
        },
        initWapks: function (data) {
            var params = {};
            var courseList = [];
            $.each(data, function (index, item) {
                courseList.push({
                    id: item.KCH,
                    name: item.KCM,
                    mainTeacher: item.SKJS
                });
            });
            params['courseList'] = courseList;
            wapks('kwglwapks', params);
        },
        refresh: function () {
            this.initKsap();
        },
        formatKscnsDatas: function (data) {
            var datas = [];
            if (data.SFQYKSCNS != "1") {
                return datas;
            }
            if (data.SFQYKSCNS_ZH == "1") {
                datas.push({language: 'ZH', language_DISPLAY: '中文', content: data.KSCNS_ZH});
            }
            if (data.SFQYKSCNS_EN == "1") {
                datas.push({language: 'EN', language_DISPLAY: '英文', content: data.KSCNS_EN});
            }
            return datas;
        },
        showKscns: function (datas, canClose) {
            var template = utils.loadCompiledPage('kscns', require);
            BH_UTILS.bhWindow(template.render({datas: datas}), "考试承诺书",
                [
                    {
                        text: canClose ? '关闭' : '我已阅读',
                        className: 'bh-btn-default',
                        callback: function () {
                            if (!canClose) {
                                $.jwAjax({
                                    url: bs.api.readCommitmentLetter,
                                    data: {XNXQDM: CACHE_DQXNXQ.XNXQDM},
                                    successMsg: '',
                                    success: function (resp) {
                                    }
                                });

                            }
                        }
                    }
                ],
                {
                    height: 600,
                    width: 900
                }
            );
        }
    };

    return viewConfig;
});
