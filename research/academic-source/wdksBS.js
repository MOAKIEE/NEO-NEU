define(function (require, exports, module) {

    var utils = require('utils');
    var bs = {
        api: {
            pageName: 'wdks',
            pageModel: 'modules/wdks.do',
            queryMyExamArrangeMent: '/api/wdks/queryMyExamArrangeMent.do',
            readCommitmentLetter: '/api/wdks/readCommitmentLetter.do',
            signin: '/api/wdks/signin.do',
            signout: '/api/wdks/signout.do'
        },
        showKssm: function (data) {
            var kssm = data.KSSM;
            if (!kssm) {
                kssm = '暂无考试说明！';
            }
            BH_UTILS.bhWindow(kssm, "考试说明",
                [
                    {
                        text: '关闭',
                        className: 'bh-btn-default',
                        callback: function () {
                            //需要定义一个空函数，以关闭弹窗
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

    return bs;
});