'use strict';
const element = (id) => document.getElementById(id);
const requestId = location.hash.slice(1);
let auth,
  confirmation,
  recaptcha,
  busy = false;
let phoneVerified = false;
let finished = false;
function showError(error) {
  const known = {
    'auth/popup-closed-by-user': '로그인이 취소됐어요. 다시 시도해 주세요.',
    'auth/popup-blocked': '팝업이 차단됐어요. 브라우저에서 팝업을 허용해 주세요.',
    'auth/invalid-verification-code': '인증번호가 올바르지 않습니다.',
    'auth/code-expired': '인증번호가 만료됐어요. 인증 문자를 다시 받아 주세요.',
    'auth/too-many-requests': '요청이 너무 많아요. 잠시 후 다시 시도해 주세요.',
    'auth/credential-already-in-use': '이 전화번호에 연결된 Google 계정으로 로그인해 주세요.',
    'auth/provider-already-linked':
      '이미 전화번호가 연결된 계정입니다. 요청을 받은 번호가 연결된 계정으로 로그인해 주세요.',
  };
  element('error').textContent =
    known[error.code] || error.message || '처리하지 못했어요. 잠시 후 다시 시도해 주세요.';
  element('error').hidden = false;
}
async function api(path, body, authenticated = true, method = 'POST') {
  const headers = { 'Content-Type': 'application/json' };
  if (authenticated) {
    if (!auth.currentUser) throw new Error('Google 계정으로 로그인해 주세요.');
    headers.Authorization = 'Bearer ' + (await auth.currentUser.getIdToken(true));
  }
  const response = await fetch('/api' + path, {
    method,
    headers,
    cache: 'no-store',
    ...(method !== 'GET' ? { body: JSON.stringify(body || {}) } : {}),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || '요청 처리에 실패했어요.');
  return data;
}
async function action(task) {
  if (busy || finished) return;
  busy = true;
  element('error').hidden = true;
  for (const button of document.querySelectorAll('button')) button.disabled = true;
  try {
    await task();
  } catch (error) {
    showError(error);
  } finally {
    busy = false;
    for (const button of document.querySelectorAll('button')) button.disabled = finished;
    element('accept').disabled = busy || finished || !phoneVerified;
  }
}
async function refreshIdentity() {
  const user = auth.currentUser;
  element('sign-in').hidden = Boolean(user);
  element('verification').hidden = !user;
  phoneVerified = false;
  if (user) {
    element('account').textContent =
      (user.email || user.displayName || 'Google 계정') + '으로 로그인했어요.';
    const state = await api(
      '/invite/identity?id=' + encodeURIComponent(requestId),
      null,
      true,
      'GET',
    );
    phoneVerified = state.phoneVerified;
    element('phone-form').hidden = phoneVerified;
    element('verified').hidden = !phoneVerified;
    if (phoneVerified)
      element('verified').textContent = '계정이 확인됐어요. 친구 요청을 수락할 수 있어요.';
  }
  element('accept').disabled = busy || !phoneVerified;
}
async function start() {
  if (!/^[a-f0-9]{64}$/.test(requestId))
    throw new Error('올바른 친구 요청 링크가 아닙니다. 받은 카카오톡의 링크를 다시 열어 주세요.');
  const preview = await api('/invite/preview', { id: requestId }, false);
  element('sender-name').textContent = preview.senderName;
  element('avatar').textContent = preview.senderName.slice(0, 1);
  element('sender-phone').textContent =
    preview.senderPhone +
    (preview.senderPhoneVerified ? ' · 인증된 연락처' : ' · 프로필 등록 연락처');
  element('recipient-phone').textContent = preview.recipientPhone || '카카오톡 공유 링크';
  element('expiry').textContent = new Date(preview.expiresAt).toLocaleDateString('ko-KR');
  element('description').textContent = preview.senderName + '님이 친구 연결을 요청했어요.';
  element('request-info').hidden = false;
  const configResponse = await fetch('/__/firebase/init.json');
  if (!configResponse.ok) throw new Error('로그인 서비스가 아직 연결되지 않았습니다.');
  firebase.initializeApp(await configResponse.json());
  auth = firebase.auth();
  auth.languageCode = 'ko';
  auth.onAuthStateChanged(() => {
    if (!finished) refreshIdentity().catch(showError);
  });
  element('login').addEventListener('click', () =>
    action(async () => {
      await auth.signInWithPopup(new firebase.auth.GoogleAuthProvider());
      await refreshIdentity();
    }),
  );
  element('logout').addEventListener('click', () =>
    action(async () => {
      confirmation = null;
      element('code-form').hidden = true;
      await auth.signOut();
      await refreshIdentity();
    }),
  );
  element('send-code').addEventListener('click', () =>
    action(async () => {
      const text = element('phone').value.trim();
      if (/[^\d +()-]/.test(text)) throw new Error('올바른 휴대폰 번호를 입력해 주세요.');
      const digits = text.replace(/\D/g, '');
      const phone = text.startsWith('+82')
        ? '+82' + digits.slice(2)
        : /^010\d{8}$/.test(digits)
          ? '+82' + digits.slice(1)
          : '';
      if (!/^\+8210\d{8}$/.test(phone))
        throw new Error('010으로 시작하는 휴대폰 번호를 입력해 주세요.');
      await api('/invite/phone-check', { id: requestId, phone });
      if (recaptcha) recaptcha.clear();
      recaptcha = new firebase.auth.RecaptchaVerifier('recaptcha', { size: 'normal' });
      confirmation = await auth.currentUser.linkWithPhoneNumber(phone, recaptcha);
      element('code-form').hidden = false;
      element('status').textContent =
        '인증 문자를 보냈어요. 문자에 표시된 6자리 번호를 입력해 주세요.';
      element('code').focus();
    }),
  );
  element('verify-code').addEventListener('click', () =>
    action(async () => {
      if (!confirmation) throw new Error('인증 문자를 먼저 받아 주세요.');
      const code = element('code').value.trim();
      if (!/^\d{6}$/.test(code)) throw new Error('6자리 인증번호를 입력해 주세요.');
      await confirmation.confirm(code);
      await refreshIdentity();
      element('status').textContent = '전화번호가 확인됐어요. 친구 요청을 수락할 수 있어요.';
    }),
  );
  element('accept').addEventListener('click', () =>
    action(async () => {
      await api('/requests/' + requestId + '/accept', {});
      finished = true;
      element('verification').hidden = true;
      element('description').textContent = '친구 연결이 완료됐어요.';
      element('status').textContent = 'SafePath 앱의 친구 탭에서 연결된 친구를 확인해 주세요.';
    }),
  );
}
start().catch((error) => {
  element('description').textContent = '요청을 확인할 수 없어요.';
  showError(error);
});
