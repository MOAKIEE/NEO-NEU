define(function (require) {

    var utils = require('utils');
    var bs = require('./wdcjBS');
    var fcsq = require('./fcsq/fcsq');
    var self = null;
    var cjxqInstance = null;
    var zcjts = require('../../public/components/zcjts/index');

    function guid() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0,
                v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    var $table = null;
    var $section = null
    var viewConfig = {

        eventMap: function () {
            return {
                '[data-action="成绩详情"]': this.actionXq,
                '[data-action="跳评教页面"]': this.actionJump
            };
        },

        initialize: function () {
            this.pushSubView([fcsq]);
            self = this;
            var indexView = utils.loadCompiledPage('wdcjIndexPage', require);
            this.$rootElement.html(indexView.render({}), true);
            $table = $('#wdcj-index');
            $section = $('#wdcj-section');
            this.initAdvancedQueryAndTable();
            this.initZpjxfjd();
            var cjxq = require('../../public/components/cjxq/index');
            cjxqInstance = cjxq('wdcj-index-cjxq');
        },

        initAdvancedQueryAndTable: function () {
            //查询头部学年学期
            $.jwAjax({
                url: bs.api.cxwdcjxnxq,
                data: {},
                successMsg: '',
                success: function (res) {
                    if (!res.rows || !res.rows.length) {
                        //渲染空页面
                        $section[0].innerHTML = "<div style='margin-top: 200px; color: #A9B0D7; font-size: 26px; text-align: center;'>同学你好！截止目前你暂无成绩记录可查询。</div>";
                        return;
                    }
                    var dqxnxqdm = '';
                    var cjxnxq = [];
                    for (var i = 0; i < res.rows.length; i++) {
                        if (res.rows[i].SFDQXNXQ == '1') {
                            dqxnxqdm = res.rows[i].XNXQDM;
                        }
                        cjxnxq.push(res.rows[i].XNXQDM);
                    }
                    var params = {};
                    if (dqxnxqdm) {
                        params.querySetting = '[{"name":"XNXQDM","value":"' + dqxnxqdm + '","builder":"m_value_equal","linkOpt":"AND"}]';
                    }
                    //中部渲染表格
                    $table.jwQueryAndTable({
                        pagePath: bs.api.pageModel,
                        action: 'cxwdcj',
                        params: params,
                        hasCheckbox: false,
                        // tpl: null,
                        // $tableDom: $('#wdcj-index-table'),
                        $searchDom: $('#wdcj-index-search'),
                        $buttonsDom: $('#wdcj-index-buttons'),
                        searchType: 'emapAdvancedQuery',
                        operationColWidth: '140px',
                        emapQuery: {
                            initComplete: function () {
                                if (dqxnxqdm) {
                                    $section.find('[data-id="' + dqxnxqdm + '"]').addClass('bh-active');
                                }
                                //把没有成绩的学年学期隐藏掉
                                var kxxnxq = $section.find('[data-name="XNXQDM"] .bh-label-radio');
                                if (kxxnxq) {
                                    for (var i = 0; i < kxxnxq.length; i++) {
                                        if (cjxnxq.indexOf($(kxxnxq[i]).attr('data-id')) < 0) {
                                            $(kxxnxq[i]).hide();
                                        }
                                    }
                                }
                            }
                        },
                        emapTable: {
                            pageable: false,
                            height: 'auto',
                            minLineNum: 1,
                            customColumns: self.getCustomColumns(),
                            // ready: function () {
                            // }
                        },
                        addExtParamBeforeReload: function (params) {
                            self.actionMyTarget(params);
                        },
                        buttons: [{
                            text: "复查申请",
                            type: "fcsq",
                            canBatch: false,
                            canSingle: true,
                            permissionKey: "wdcj-fc",
                            callback: self.actionFc,
                            beforeRender: function (options, rowData) {
                                return rowData.FCSQ;
                            }
                        }, {
                            text: "复查中",
                            type: "fcz",
                            canBatch: false,
                            canSingle: true,
                            permissionKey: "wdcj-fc",
                            callback: self.actionFc,
                            beforeRender: function (options, rowData) {
                                return rowData.FCZ;
                            }
                        }, {
                            text: "复查结果",
                            type: "fcjg",
                            canBatch: false,
                            canSingle: true,
                            permissionKey: "wdcj-fc",
                            callback: self.actionFc,
                            beforeRender: function (options, rowData) {
                                return rowData.FCJG;
                            }
                        }, {
                            text: "分项成绩",
                            canBatch: false,
                            canSingle: true,
                            permissionKey: "wdcj-fxcj",
                            callback: self.actionFxcj,
                            beforeRender: function (options, rowData) {
                                return rowData.FXCJ;
                            }
                        }]
                    });
                    self.actionMyTarget(params);
                }
            });
        },
        initZpjxfjd:function(){
            $.jwAjax({
            	url: '/sys/cjzhcxapp/api/wdcj/queryPjxfjd.do',
            	data: {},
            	async: false,
            	successMsg: '',
            	success: function(resp){
            		$("#wdcj-zpjxfjd").html("总平均学分绩点："+resp.ZPJXFJD);
            	}
            });    
        },
        getCustomColumns: function () {
            return [{
                colField: 'XSZCJ',
                type: 'tpl',
                column: {
                    sortable: false,
                    align: 'center',
                    cellsalign: 'center',
                    cellsRenderer: function (row, column, value, rowData) {
                        if (rowData.WPJ) {
                            return '<a href="javascript:void(0);" class="j-row-edit" data-action="跳评教页面">' + value || '' + '</a>';
                        } else if (rowData.KCKXQ) {
                            var redClass = '';
                            if (rowData.SFJG == '0') {
                                redClass = 'bh-color-danger';
                            }
                            return '<a href="javascript:void(0);" class="j-row-edit ' + redClass + '" data-wid="' + rowData.WID + '" data-action="成绩详情">' + value + '</a>';
                        } else if (rowData.WKFMS) {
                            return self.getShowDiv(rowData.WKFMS, value);
                        } else {
                            return value || '';
                        }
                    }
                }
            }, {
                colField: 'JD',
                type: 'tpl',
                column: {
                    sortable: false,
                    align: 'center',
                    cellsalign: 'center',
                    cellsRenderer: function (row, column, value, rowData) {
                        return value || '';
                    }
                }
            }];
        },

        getShowDiv: function (wkfms, value) {
            var className = 'A' + guid();
            (function (className, text, tip) {
                setTimeout(function () {
                    if ($('.' + className).length > 0) {
                        zcjts($('.' + className)[1], {text: text, tip: tip});
                    }
                }, 0);
            })(className, value, wkfms);
            return '<div class="' + className + '"></div>';
        },

        actionMyTarget: function (params) {
            $.jwAjax({
                url: bs.api.myTarget,
                data: params,
                successMsg: '',
                success: function (res) {
                    //渲染下面的指标
                    var wdzb = require('../../public/components/wdzb/index');
                    wdzb('wdcj-index-bottom', {
                        data: res
                    });
                }
            });
        },

        actionFc: function (checkedOrSearchQuerySetting) {
            fcsq.initialize(checkedOrSearchQuerySetting.records[0], this);
        },

        actionFxcj: function (checkedOrSearchQuerySetting) {
            self.actionView(checkedOrSearchQuerySetting.records[0].WID);
        },

        actionXq: function (event) {
            self.actionView($(event.currentTarget).attr('data-wid'));
        },

        actionJump: function () {
            JW_UTILS.goTo('pjapp', 'wdpj', {});
        },

        actionView: function (wid) {
            //查询详情接口
            $.jwAjax({
                url: bs.api.details,
                data: {WID: wid},
                successMsg: '',
                success: function (res) {
                    cjxqInstance.instance.show(res);
                }
            });
        }

    };

    return viewConfig;
});
