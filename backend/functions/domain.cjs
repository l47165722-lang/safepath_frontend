const { randomBytes, createHash } = require('node:crypto');

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}
function normalizePhone(value) {
  if (typeof value !== 'string' || /[^\d +()-]/.test(value)) return null;
  let digits = value.replace(/\D/g, '');
  if (value.trim().startsWith('+82')) digits = '0' + digits.slice(2);
  else if (value.trim().startsWith('0082')) digits = '0' + digits.slice(4);
  return /^010\d{8}$/.test(digits) ? '+82' + digits.slice(1) : null;
}
function name(value) {
  if (
    typeof value !== 'string' ||
    !value.trim() ||
    value.trim().length > 50 ||
    /[\r\n\u0000-\u001f]/.test(value)
  ) {
    throw new ApiError(400, '이름은 1~50자로 입력해 주세요.');
  }
  return value.trim();
}
function validateDraft(body) {
  const draft = {
    senderName: name(body.senderName),
    senderPhone: normalizePhone(body.senderPhone),
    submissionId: body.submissionId,
  };
  if (!draft.senderPhone) throw new ApiError(400, '내 정보의 전화번호를 확인해 주세요.');
  if (typeof draft.submissionId !== 'string' || !/^[0-9a-f-]{36}$/.test(draft.submissionId)) {
    throw new ApiError(400, '요청 식별자가 올바르지 않습니다.');
  }
  return draft;
}
function requirePending(request, now) {
  if (!request) throw new ApiError(404, '친구 요청을 찾을 수 없습니다.');
  if (request.status !== 'pending') throw new ApiError(409, '이미 처리되었거나 취소된 요청입니다.');
  if (request.expiresAt <= now)
    throw new ApiError(410, '만료된 친구 요청입니다. 새 요청을 받아 주세요.');
}
function authorizeRecipient(request, user) {
  if (request.senderUid === user.uid)
    throw new ApiError(403, '내가 보낸 요청은 수락할 수 없습니다.');
  if (request.deliveryChannel === 'kakao_share') {
    if (!(request.viewerUids || []).includes(user.uid))
      throw new ApiError(403, '받은 요청 링크에서 로그인 후 확인해 주세요.');
    return;
  }
  if (normalizePhone(user.phoneNumber || '') !== request.recipientPhone) {
    throw new ApiError(403, '요청을 받은 전화번호로 본인 인증 후 수락해 주세요.');
  }
}
function maskedPhone(value) {
  return '010-****-' + (value || '').slice(-4);
}
function id() {
  return randomBytes(32).toString('hex');
}
function key(value) {
  return createHash('sha256').update(value).digest('hex');
}
module.exports = {
  ApiError,
  normalizePhone,
  validateDraft,
  requirePending,
  authorizeRecipient,
  maskedPhone,
  id,
  key,
};
