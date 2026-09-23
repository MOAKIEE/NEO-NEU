;(function (window) {
    var self;
    var AuthKit = {
        _init: function () {
            self = this;
        },
        /**
         * 是否是教务处任职类别
         * @param rzlbdm
         */
        isJwc: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '10');
        },
        /**
         * 是否是院系任职类别
         * @param rzlbdm
         */
        isYx: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '30');
        },
        /**
         * 是否是院系任职类别
         * @param rzlbdm
         */
        isJs: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '80');
        },
        /**
         * 是否是院系任职类别
         * @param rzlbdm
         */
        isZy: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '60');
        },
        /**
         * 是否是院系任职类别
         * @param rzlbdm
         */
        isXs: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '90');
        },
        /**
         * 是否是院系任职类别
         * @param rzlbdm
         */
        isJys: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '40');
        },
        /**
         * 是否是院系任职类别
         * @param rzlbdm
         */
        isBj: function (rzlbdm) {
            return isThisRzlb(rzlbdm, '50');
        }
    };

    /**
     * 是否是此任职类别
     * @param rzlbdm 不传则取当前登录人用户组任职类别
     * @param prefix
     * @returns {*}
     */
    function isThisRzlb(rzlbdm, prefix) {
        if (!rzlbdm) {
            rzlbdm = _JW_INIT_CONFIG.rzlbdm;
        }
        return _.startsWith(rzlbdm, prefix);
    }


    AuthKit._init();
    window.AuthKit = AuthKit;
})(window);