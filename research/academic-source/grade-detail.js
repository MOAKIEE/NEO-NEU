/**
 * Created by jwu on 2023/11/16
 * E-mail:01313101@wisedu.com
 *
 * Description:"成绩详情"抽屉组件
 */

define(function (require, exports, module) {
    var echarts = require('../../js/echarts.min');

    function init(rootElementId) {
        var vueInstance = {instance: null};

        A.loadElementUI(function () {
            require('css!./index.css');
            var tpl = require("text!./index.vue");

            $("#" + rootElementId).html(tpl);

            vueInstance.instance = new Vue({
                el: '#' + rootElementId,
                data: function () {
                    return {
                        drawerVisible: false,// 抽屉是否可见
                        data: null,
                        currentTab: 'teachingClass',// tab 默认为“教学班统计”
                        scoreStatistics: null//当前展示的统计数据
                    };
                },
                mounted: function () {

                },
                methods: {
                    show: function (data) {
                        this.drawerVisible = true;
                        this.data = data;
                        this.currentTab = data.scoreStatisticsTeachingClass ? 'teachingClass' : 'school';
                        this.scoreStatistics = data.scoreStatisticsTeachingClass || data.scoreStatisticsSchool;

                        var that = this;
                        this.$nextTick(function () {
                            that._initChart();
                        });
                    },
                    _close: function () {
                        this.drawerVisible = false;
                        this.data = null;
                        this.currentTab = 'teachingClass';
                        this.scoreStatistics = null;
                    },
                    _initChart: function () {
                        if (!this.scoreStatistics || this.scoreStatistics.scoreRanges.length === 0) {
                            return;
                        }
                        var instance = echarts.init(document.getElementById('wdcj-cjxq-drawer-chart-root'));

                        var xData = [];
                        var yData = [];
                        for (var i = 0; i < this.scoreStatistics.scoreRanges.length; i++) {
                            xData.push(this.scoreStatistics.scoreRanges[i].name);
                            yData.push(this.scoreStatistics.scoreRanges[i].value);
                        }

                        instance.setOption({
                            tooltip: {
                                trigger: 'axis',
                                axisPointer: {
                                    type: 'shadow'
                                }
                            },
                            grid: {
                                left: '8%',
                                right: '8%',
                                bottom: '10%',
                                containLabel: true
                            },
                            xAxis: [
                                {
                                    type: 'category',
                                    data: xData,
                                    axisTick: {
                                        alignWithLabel: true
                                    },
                                    name: '分数区间（分）',
                                    nameLocation: 'center',
                                    nameTextStyle: {
                                        padding: [10, 0, 0, 0]
                                    }
                                }
                            ],
                            yAxis: [
                                {
                                    type: 'value',
                                    name: '学生人数（人）',
                                    minInterval: 1,
                                    axisLine: {
                                        show: true
                                    }
                                }
                            ],
                            series: [
                                {
                                    name: '人数',
                                    type: 'bar',
                                    barWidth: '50%',
                                    data: yData
                                }
                            ]
                        });
                    },
                    _switchTab: function (key) {
                        if (this.currentTab === key) {
                            return;
                        }

                        this.currentTab = key;

                        switch (key) {
                            case 'teachingClass':
                                this.scoreStatistics = this.data.scoreStatisticsTeachingClass;
                                break;
                            case 'school':
                                this.scoreStatistics = this.data.scoreStatisticsSchool;
                                break;
                        }

                        var that = this;
                        this.$nextTick(function () {
                            that._initChart();
                        });
                    }
                }
            });
        });

        return vueInstance;
    }

    return init;
});