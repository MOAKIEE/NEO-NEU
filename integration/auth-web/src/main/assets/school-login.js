// Evaluated only in the top-level official CAS page. Returns fixed states, never field values.
(function (account, password, allowSubmit, alreadySubmitted) {
    'use strict';
    if (location.origin !== 'https://pass.neu.edu.cn' || location.pathname !== '/tpass/login') return 'unsupported';
    function visible(element) {
        if (!element || !element.getClientRects().length) return false;
        var style = getComputedStyle(element);
        return style.display !== 'none' && style.visibility !== 'hidden' && style.visibility !== 'collapse';
    }
    var form = document.getElementById('loginForm');
    var user = document.getElementById('un');
    var pass = document.getElementById('pd');
    var button = document.getElementById('index_login_btn');
    var error = document.getElementById('errormsg');
    var hiddenError = document.getElementById('errormsghide');
    var errorText = ((visible(error) && error.textContent) || (hiddenError && hiddenError.textContent) || '').trim();
    var challenge = Array.prototype.some.call(document.querySelectorAll(
        '#codeImage, #mcode, #second_valid_ok, #sendConfirm, input[name="captcha"], input[name="vcode"], ' +
        'input[name="code"], input[id*="captcha"], iframe[src*="captcha"], [class*="geetest"], [class*="captcha"]'
    ), visible) || /验证码|二次认证|动态口令|安全验证/.test(errorText);
    if (errorText && !challenge) return 'rejected';
    if (!form || !user || !pass || !button || user.form !== form || pass.form !== form ||
        pass.type !== 'password' || (form.method || '').toLowerCase() !== 'post') {
        return challenge ? 'challenge' : 'unsupported';
    }
    var action = new URL(form.action, location.href);
    if (action.origin !== location.origin || action.pathname !== '/tpass/login' ||
        (form.target && form.target !== '_self') || button.formAction && button.hasAttribute('formaction') ||
        button.formTarget && button.hasAttribute('formtarget')) return 'unsupported';
    if (alreadySubmitted || window.__neoNeuLoginSubmitted) return challenge ? 'challenge' : 'waiting';
    if (account !== null && password !== null) {
        user.value = account;
        pass.value = password;
        user.dispatchEvent(new Event('input', { bubbles: true }));
        pass.dispatchEvent(new Event('input', { bubbles: true }));
    }
    if (challenge) return 'challenge';
    if (user.disabled || pass.disabled || button.disabled || typeof window.login !== 'function') return 'unsupported';
    if (!allowSubmit) return 'form';
    // Use the official handler so its current RSA/encryption and validation remain authoritative.
    window.__neoNeuLoginSubmitted = true;
    button.click();
    return 'submitted';
})
