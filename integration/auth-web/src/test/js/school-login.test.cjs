const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname, '../../main/assets/school-login.js'), 'utf8');

// Synthetic DOM modeled on the public CAS selectors; no real credentials, tickets or responses.
function fixture() {
    const form = { method: 'post', action: 'https://pass.neu.edu.cn/tpass/login', target: '' };
    const user = { form, disabled: false, dispatchEvent() {} };
    const pass = { form, type: 'password', disabled: false, dispatchEvent() {} };
    let clicks = 0;
    const button = { disabled: false, hasAttribute() { return false; }, click() { clicks++; } };
    const fields = { loginForm: form, un: user, pd: pass, index_login_btn: button };
    const context = {
        location: { origin: 'https://pass.neu.edu.cn', pathname: '/tpass/login', href: 'https://pass.neu.edu.cn/tpass/login' },
        window: { login() {} }, URL, Event: class {},
        getComputedStyle(el) { return { display: el.hidden ? 'none' : 'block', visibility: 'visible' }; },
        document: { getElementById(id) { return fields[id]; }, querySelectorAll() { return context.challenges || []; } }
    };
    const run = vm.runInNewContext(script, context);
    return { context, form, user, pass, button, fields, run, clicks: () => clicks };
}
function visible() { return { getClientRects() { return [{}]; } }; }
let tests = 0;
function test(name, fn) { fn(); tests++; console.log(`PASS ${name}`); }

test('inspection does not read or submit account/password values', () => {
    const f = fixture();
    for (const el of [f.user, f.pass]) Object.defineProperty(el, 'value', { get() { throw Error('secret read'); } });
    assert.equal(f.run(null, null, false, false), 'form');
    assert.equal(f.clicks(), 0);
});

test('a late school handler is not mistaken for a ready password form', () => {
    const f = fixture();
    delete f.context.window.login;
    assert.equal(f.run(null, null, false, false), 'unsupported');
    assert.equal(f.clicks(), 0);
    f.context.window.login = function () {};
    assert.equal(f.run(null, null, false, false), 'form');
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'submitted');
    assert.equal(f.clicks(), 1);
});
test('official handler is clicked once even if the page is polled again', () => {
    const f = fixture();
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'submitted');
    assert.equal(f.user.value, 'synthetic');
    assert.equal(f.pass.value, 'test-secret');
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'waiting');
    assert.equal(f.clicks(), 1);
});
test('visible CAPTCHA fills credentials and waits for human without clicking', () => {
    const f = fixture(); f.context.challenges = [visible()];
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'challenge');
    assert.equal(f.pass.value, 'test-secret'); assert.equal(f.clicks(), 0);
});
test('hidden second-factor template does not force an interactive fallback', () => {
    const f = fixture(); f.context.challenges = [{ ...visible(), hidden: true }];
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'submitted');
});
test('second factor added after submit is detected without resubmitting', () => {
    const f = fixture(); f.run('synthetic', 'test-secret', true, false);
    f.context.challenges = [visible()];
    assert.equal(f.run(null, null, false, true), 'challenge'); assert.equal(f.clicks(), 1);
});
test('a returned login form does not retry credentials after navigation', () => {
    const f = fixture(); assert.equal(f.run('synthetic', 'test-secret', true, true), 'waiting');
    assert.equal(f.clicks(), 0); assert.equal(f.pass.value, undefined);
});
test('credential rejection is surfaced without returning error contents', () => {
    const f = fixture(); f.fields.errormsg = { ...visible(), textContent: 'synthetic account rejected' };
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'rejected');
    assert.equal(f.pass.value, undefined); assert.equal(f.clicks(), 0);
});
test('CAPTCHA error is interactive rather than a rejected password', () => {
    const f = fixture(); f.fields.errormsg = { ...visible(), textContent: '验证码错误' };
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'challenge');
});
test('untrusted origin, port and path cannot receive credentials', () => {
    for (const patch of [{ origin: 'https://evil.example' }, { origin: 'http://pass.neu.edu.cn' },
        { origin: 'https://pass.neu.edu.cn:8443' }, { pathname: '/other' }]) {
        const f = fixture(); Object.assign(f.context.location, patch);
        assert.equal(f.run('synthetic', 'test-secret', true, false), 'unsupported');
        assert.equal(f.pass.value, undefined); assert.equal(f.clicks(), 0);
    }
});
test('external form action, different endpoint and form target cannot receive credentials', () => {
    for (const patch of [{ action: 'https://evil.example/tpass/login' }, { action: 'https://pass.neu.edu.cn/other' }, { target: '_blank' }]) {
        const f = fixture(); Object.assign(f.form, patch);
        assert.equal(f.run('synthetic', 'test-secret', true, false), 'unsupported'); assert.equal(f.pass.value, undefined);
    }
});
test('button overrides and unrecognized form structure stop automation', () => {
    const f = fixture(); f.button.formAction = 'https://evil.example'; f.button.hasAttribute = () => true;
    assert.equal(f.run('synthetic', 'test-secret', true, false), 'unsupported'); assert.equal(f.pass.value, undefined);
    const g = fixture(); g.pass.form = {};
    assert.equal(g.run('synthetic', 'test-secret', true, false), 'unsupported');
});
console.log(`${tests} login DOM tests passed`);
