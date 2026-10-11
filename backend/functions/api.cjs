const express = require('express');
const {
  ApiError,
  validateDraft,
  requirePending,
  authorizeRecipient,
  maskedPhone,
  id,
  key,
  normalizePhone,
} = require('./domain.cjs');

function createApi({ db, auth, publicOrigin, now = () => Date.now() }) {
  if (!/^https:\/\/[^/]+$/.test(publicOrigin)) throw new Error('HTTPS public origin required');
  const app = express();
  app.disable('x-powered-by');
  app.use(express.json({ limit: '8kb' }));
  app.use((req, res, next) => {
    res.set('Cache-Control', 'no-store');
    res.set('Referrer-Policy', 'no-referrer');
    next();
  });
  const router = express.Router();
  const requests = db.collection('friendRequests');
  const friendRef = (uid, otherUid) =>
    db.collection('friendUsers').doc(uid).collection('friends').doc(otherUid);
  const url = (requestId) => `${publicOrigin}/invite/#${requestId}`;
  const safeId = (value) => {
    if (typeof value !== 'string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(value))
      throw new ApiError(400, '잘못된 요청입니다.');
    return value;
  };
  const publicRequest = (request, requestId) => ({
    id: requestId,
    url: url(requestId),
    senderName: request.senderName,
    deliveryStatus: request.deliveryStatus,
    deliveryChannel: request.deliveryChannel,
  });

  async function signedIn(req, res, next) {
    try {
      const bearer = req.get('authorization') || '';
      if (!bearer.startsWith('Bearer ')) throw new ApiError(401, 'Google 로그인이 필요합니다.');
      let token;
      try {
        token = await auth.verifyIdToken(bearer.slice(7), true);
      } catch {
        throw new ApiError(401, '로그인 인증이 만료되었습니다. 다시 로그인해 주세요.');
      }
      req.user = await auth.getUser(token.uid);
      next();
    } catch (error) {
      next(error);
    }
  }

  router.post('/invite/preview', async (req, res) => {
    const requestId = safeId(req.body?.id);
    const snapshot = await requests.doc(requestId).get();
    const request = snapshot.data();
    requirePending(request, now());
    res.json({
      senderName: request.senderName,
      senderPhone: maskedPhone(request.senderPhone),
      senderPhoneVerified: request.senderPhoneVerified,
      recipientPhone: maskedPhone(request.recipientPhone),
      expiresAt: request.expiresAt,
    });
  });

  router.get('/invite/identity', signedIn, async (req, res) => {
    const requestId = safeId(req.query.id);
    const request = await db.runTransaction(async (tx) => {
      const ref = requests.doc(requestId);
      const snapshot = await tx.get(ref);
      const data = snapshot.data();
      requirePending(data, now());
      if (data.senderUid === req.user.uid) throw new ApiError(403, '내가 보낸 요청입니다.');
      if (data.deliveryChannel === 'kakao_share') {
        const viewers = data.viewerUids || [];
        if (!viewers.includes(req.user.uid)) {
          if (viewers.length >= 20)
            throw new ApiError(429, '요청 링크 확인 한도에 도달했습니다. 새 요청을 받아 주세요.');
          tx.update(ref, { viewerUids: [...viewers, req.user.uid] });
        }
      }
      return data;
    });
    res.json({
      phoneVerified:
        request.deliveryChannel === 'kakao_share' ||
        normalizePhone(req.user.phoneNumber || '') === request.recipientPhone,
    });
  });

  router.post('/invite/phone-check', signedIn, async (req, res) => {
    const snapshot = await requests.doc(safeId(req.body?.id)).get();
    const request = snapshot.data();
    requirePending(request, now());
    if (normalizePhone(req.body?.phone || '') !== request.recipientPhone) {
      throw new ApiError(403, '친구 요청 알림톡을 받은 전화번호를 입력해 주세요.');
    }
    if (req.user.uid === request.senderUid) throw new ApiError(403, '내가 보낸 요청입니다.');
    res.json({ matches: true });
  });

  router.post('/requests/share', signedIn, async (req, res) => {
    const draft = validateDraft(req.body || {});
    const timestamp = now();
    const fingerprint = key(JSON.stringify([draft.senderName, draft.senderPhone]));
    const submissionRef = db
      .collection('friendRequestSubmissions')
      .doc(key(req.user.uid + ':' + draft.submissionId));
    const limitRef = db
      .collection('friendRequestLimits')
      .doc(key(req.user.uid + ':' + new Date(timestamp).toISOString().slice(0, 10)));
    const requestId = id();
    const request = {
      senderUid: req.user.uid,
      senderName: draft.senderName,
      senderPhone: normalizePhone(req.user.phoneNumber || '') || draft.senderPhone,
      senderPhoneVerified: Boolean(normalizePhone(req.user.phoneNumber || '')),
      recipientName: '카카오톡 친구',
      viewerUids: [],
      status: 'pending',
      deliveryStatus: 'ready',
      deliveryChannel: 'kakao_share',
      createdAt: timestamp,
      expiresAt: timestamp + 7 * 24 * 60 * 60 * 1000,
    };
    const previous = await db.runTransaction(async (tx) => {
      const submission = await tx.get(submissionRef);
      if (submission.exists) {
        if (submission.data().fingerprint !== fingerprint)
          throw new ApiError(409, '입력 내용이 변경되었습니다. 다시 요청해 주세요.');
        const original = await tx.get(requests.doc(submission.data().requestId));
        requirePending(original.data(), timestamp);
        return { ...original.data(), id: original.id };
      }
      const limit = await tx.get(limitRef);
      const count = limit.data()?.count || 0;
      if (count >= 20) throw new ApiError(429, '오늘의 친구 요청 한도에 도달했습니다.');
      tx.set(requests.doc(requestId), request);
      tx.set(submissionRef, { requestId, fingerprint });
      tx.set(limitRef, { count: count + 1 });
      return null;
    });
    res
      .status(previous ? 200 : 201)
      .json(publicRequest(previous || request, previous?.id || requestId));
  });

  router.get('/friends', signedIn, async (req, res) => {
    const phone = normalizePhone(req.user.phoneNumber || '');
    const [friends, sent, received, shared] = await Promise.all([
      db.collection('friendUsers').doc(req.user.uid).collection('friends').get(),
      requests.where('senderUid', '==', req.user.uid).get(),
      phone ? requests.where('recipientPhone', '==', phone).get() : Promise.resolve({ docs: [] }),
      requests.where('viewerUids', 'array-contains', req.user.uid).get(),
    ]);
    const pending = (snapshots, nameKey) =>
      snapshots.docs
        .filter((doc) => {
          const data = doc.data();
          return data.status === 'pending' && data.expiresAt > now();
        })
        .sort((a, b) => b.data().createdAt - a.data().createdAt)
        .map((doc) => ({
          id: doc.id,
          name: doc.data()[nameKey],
          deliveryStatus: doc.data().deliveryStatus,
          createdAt: doc.data().createdAt,
        }));
    res.json({
      friends: friends.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
      received: pending(
        {
          docs: [
            ...new Map([...received.docs, ...shared.docs].map((doc) => [doc.id, doc])).values(),
          ],
        },
        'senderName',
      ),
      sent: pending(sent, 'recipientName'),
    });
  });

  router.post('/requests/:id/disconnect', signedIn, async (req, res) => {
    const otherUid = safeId(req.params.id);
    await db.runTransaction(async (tx) => {
      const own = friendRef(req.user.uid, otherUid);
      const snapshot = await tx.get(own);
      if (!snapshot.exists) throw new ApiError(404, '연결된 친구를 찾을 수 없습니다.');
      tx.delete(own);
      tx.delete(friendRef(otherUid, req.user.uid));
    });
    res.json({ status: 'disconnected' });
  });

  router.post('/requests/:id/:action', signedIn, async (req, res) => {
    const requestId = safeId(req.params.id);
    const action = req.params.action;
    if (!['accept', 'reject', 'cancel'].includes(action))
      throw new ApiError(404, '잘못된 요청입니다.');
    const result = await db.runTransaction(async (tx) => {
      const requestRef = requests.doc(requestId);
      const snapshot = await tx.get(requestRef);
      const request = snapshot.data();
      if (
        action === 'accept' &&
        request?.status === 'accepted' &&
        request.recipientUid === req.user.uid
      ) {
        const connected = await tx.get(friendRef(req.user.uid, request.senderUid));
        if (!connected.exists)
          throw new ApiError(409, '이미 수락 후 연결이 해제된 요청입니다. 새 요청을 받아 주세요.');
        return 'accepted';
      }
      requirePending(request, now());
      if (action === 'cancel') {
        if (request.senderUid !== req.user.uid)
          throw new ApiError(403, '내가 보낸 요청만 취소할 수 있습니다.');
      } else authorizeRecipient(request, req.user);
      const status = { accept: 'accepted', reject: 'rejected', cancel: 'cancelled' }[action];
      if (action === 'accept') {
        const existing = await tx.get(friendRef(request.senderUid, req.user.uid));
        if (existing.exists) throw new ApiError(409, '이미 연결된 친구입니다.');
        // Both friendship edges and the request are committed together.
        tx.set(friendRef(request.senderUid, req.user.uid), {
          name:
            request.deliveryChannel === 'kakao_share'
              ? (req.user.displayName || '친구').slice(0, 50)
              : request.recipientName,
          phone:
            request.deliveryChannel === 'kakao_share'
              ? normalizePhone(req.user.phoneNumber || '') || ''
              : request.recipientPhone,
          connectedAt: now(),
        });
        tx.set(friendRef(req.user.uid, request.senderUid), {
          name: request.senderName,
          phone: request.senderPhone,
          connectedAt: now(),
        });
      }
      tx.update(requestRef, {
        status,
        handledAt: now(),
        ...(action === 'accept' ? { recipientUid: req.user.uid } : {}),
      });
      return status;
    });
    res.json({ status: result });
  });
  app.use('/api', router);
  app.use((req, res) => res.status(404).json({ error: '요청한 주소를 찾을 수 없습니다.' }));
  app.use((error, req, res, next) => {
    const status =
      error instanceof ApiError ? error.status : error.type === 'entity.parse.failed' ? 400 : 500;
    if (status === 500)
      console.error('Friends API failure', { name: error.name, code: error.code || null });
    res.status(status).json({
      error:
        status === 500
          ? '요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
          : status === 400 && !(error instanceof ApiError)
            ? '잘못된 요청입니다.'
            : error.message,
    });
  });
  return app;
}
module.exports = { createApi };
