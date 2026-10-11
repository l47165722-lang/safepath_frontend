const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { createApi } = require('../api.cjs');
const { normalizePhone } = require('../domain.cjs');

// Enforce Firestore's read-before-write transaction rule in this API fixture.
class Database {
  constructor() {
    this.data = new Map();
    this.queue = Promise.resolve();
  }
  collection(path) {
    const db = this;
    const query = (filter) => ({
      get: async () => ({
        docs: [...db.data]
          .filter(
            ([key, value]) =>
              key.startsWith(path + '/') &&
              !key.slice(path.length + 1).includes('/') &&
              (!filter || filter(value)),
          )
          .map(([key, value]) => ({
            id: key.split('/').at(-1),
            data: () => structuredClone(value),
          })),
      }),
    });
    return {
      doc: (id) => db.doc(path + '/' + id),
      get: query().get,
      where: (field, op, value) => {
        assert.ok(['==', 'array-contains'].includes(op));
        return query((row) =>
          op === '==' ? row[field] === value : (row[field] || []).includes(value),
        );
      },
    };
  }
  doc(path) {
    return {
      path,
      id: path.split('/').at(-1),
      collection: (name) => this.collection(path + '/' + name),
      get: async () => this.snapshot(path),
      update: async (value) => this.data.set(path, { ...this.data.get(path), ...value }),
    };
  }
  snapshot(path) {
    return {
      exists: this.data.has(path),
      id: path.split('/').at(-1),
      data: () => structuredClone(this.data.get(path)),
    };
  }
  runTransaction(callback) {
    const run = this.queue.then(async () => {
      const writes = [];
      const result = await callback({
        get: async (ref) => {
          assert.equal(writes.length, 0, 'transaction reads must precede writes');
          return this.snapshot(ref.path);
        },
        set: (ref, value) => writes.push(() => this.data.set(ref.path, structuredClone(value))),
        update: (ref, value) =>
          writes.push(() => this.data.set(ref.path, { ...this.data.get(ref.path), ...value })),
        delete: (ref) => writes.push(() => this.data.delete(ref.path)),
      });
      writes.forEach((write) => write());
      return result;
    });
    this.queue = run.catch(() => {});
    return run;
  }
}

const timestamp = 1_800_000_000_000;
const users = {
  sender: { uid: 'sender' },
  receiver: { uid: 'receiver', phoneNumber: '+821012345678' },
  stranger: { uid: 'stranger', phoneNumber: '+821099998888' },
};
const auth = {
  verifyIdToken: async (token) => {
    if (!users[token]) throw new Error('invalid');
    return { uid: token };
  },
  getUser: async (uid) => users[uid],
};
let db, server, base, available, failAlimtalk, alimtalkCount;
before(async () => {
  db = new Database();
  const api = createApi({
    db: {
      collection: (...args) => db.collection(...args),
      runTransaction: (...args) => db.runTransaction(...args),
    },
    auth,
    publicOrigin: 'https://example.test',
    now: () => timestamp,
    getNotificationSender: () =>
      available
        ? {
            send: async () => {
              alimtalkCount++;
              if (failAlimtalk) throw new Error('timeout');
              return { messageId: 'receipt' };
            },
          }
        : null,
  });
  await new Promise((resolve) => {
    server = api.listen(0, '127.0.0.1', resolve);
  });
  base = `http://127.0.0.1:${server.address().port}/api`;
});
after(async () => {
  server.closeAllConnections();
  await new Promise((resolve) => server.close(resolve));
});
beforeEach(() => {
  db.data.clear();
  db.queue = Promise.resolve();
  available = true;
  failAlimtalk = false;
  alimtalkCount = 0;
});
async function call(path, user = 'sender', body, method = 'POST') {
  const response = await fetch(base + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(user ? { Authorization: `Bearer ${user}` } : {}),
    },
    ...(method !== 'GET' ? { body: JSON.stringify(body || {}) } : {}),
  });
  return { status: response.status, body: await response.json() };
}
const draft = {
  name: '친구',
  phone: '01012345678',
  senderName: '요청자',
  senderPhone: '01011112222',
  submissionId: '11111111-1111-4111-8111-111111111111',
};
async function create() {
  const result = await call('/requests/share', 'sender', draft);
  assert.equal(result.status, 201);
  return result.body.id;
}

test('unauthenticated creation is rejected', async () => {
  assert.equal((await call('/requests/share', null, draft)).status, 401);
  assert.equal(db.data.size, 0);
});
test('sharing creates a link without an Alimtalk account or message delivery claim', async () => {
  available = false;
  const result = await call('/requests/share', 'sender', draft);
  assert.equal(result.status, 201);
  assert.equal(result.body.deliveryChannel, 'kakao_share');
  assert.equal(result.body.deliveryStatus, 'ready');
  assert.equal(alimtalkCount, 0);
  assert.match(result.body.url, /^https:\/\/example.test\/invite\/#[a-f0-9]{64}$/);
});
test('concurrent retries reuse the same link and quota', async () => {
  const results = await Promise.all([
    call('/requests/share', 'sender', draft),
    call('/requests/share', 'sender', draft),
  ]);
  assert.deepEqual(results.map((r) => r.status).sort(), [200, 201]);
  assert.equal(results[0].body.id, results[1].body.id);
  assert.equal([...db.data].filter(([k]) => k.startsWith('friendRequests/')).length, 1);
});
test('preview masks sender contact and has no selected recipient phone', async () => {
  const id = await create();
  const preview = await call('/invite/preview', null, { id });
  assert.equal(preview.body.senderName, '요청자');
  assert.equal(preview.body.senderPhone, '010-****-2222');
  assert.ok(!JSON.stringify(preview.body).includes('+8210'));
  assert.equal(db.data.get('friendRequests/' + id).recipientPhone, undefined);
});
async function open(id, user = 'receiver') {
  return call('/invite/identity?id=' + id, user, null, 'GET');
}
test('sender and accounts that have not opened the link cannot accept', async () => {
  const id = await create();
  assert.equal((await open(id, 'sender')).status, 403);
  assert.equal((await call(`/requests/${id}/accept`, 'receiver')).status, 403);
  assert.equal((await call(`/requests/${id}/accept`, null)).status, 401);
});
test('authenticated link opening adds received notification without selecting a recipient', async () => {
  const id = await create();
  assert.equal((await open(id)).body.phoneVerified, true);
  assert.equal((await call('/friends', 'receiver', null, 'GET')).body.received.length, 1);
  assert.equal((await call('/friends', 'stranger', null, 'GET')).body.received.length, 0);
  assert.equal(db.data.get('friendRequests/' + id).recipientUid, undefined);
});
test('acceptance atomically connects both accounts and retry is idempotent', async () => {
  const id = await create();
  await open(id);
  assert.equal((await call(`/requests/${id}/accept`, 'receiver')).status, 200);
  assert.equal((await call(`/requests/${id}/accept`, 'receiver')).status, 200);
  assert.equal(db.data.get('friendUsers/receiver/friends/sender').name, '요청자');
  assert.equal(db.data.get('friendUsers/sender/friends/receiver').phone, '+821012345678');
  assert.equal((await call('/friends', 'receiver', null, 'GET')).body.received.length, 0);
});
test('forwarded link can be accepted only once under simultaneous acceptance', async () => {
  const id = await create();
  await open(id);
  await open(id, 'stranger');
  const results = await Promise.all([
    call(`/requests/${id}/accept`, 'receiver'),
    call(`/requests/${id}/accept`, 'stranger'),
  ]);
  assert.deepEqual(results.map((r) => r.status).sort(), [200, 409]);
  assert.equal([...db.data].filter(([k]) => k.startsWith('friendUsers/sender/friends/')).length, 1);
});
test('Google authenticated recipient does not need phone OTP for shared links', async () => {
  users.googleOnly = { uid: 'googleOnly', displayName: '새 친구' };
  const id = await create();
  await open(id, 'googleOnly');
  assert.equal((await call(`/requests/${id}/accept`, 'googleOnly')).status, 200);
  assert.equal(db.data.get('friendUsers/sender/friends/googleOnly').name, '새 친구');
  assert.equal(db.data.get('friendUsers/sender/friends/googleOnly').phone, '');
});
test('cancellation and expiry prevent acceptance', async () => {
  const id = await create();
  await open(id);
  assert.equal((await call(`/requests/${id}/cancel`, 'stranger')).status, 403);
  await call(`/requests/${id}/cancel`, 'sender');
  assert.equal((await call(`/requests/${id}/accept`, 'receiver')).status, 409);
  db.data.set('friendRequests/' + id, {
    ...db.data.get('friendRequests/' + id),
    status: 'pending',
    expiresAt: timestamp - 1,
  });
  assert.equal((await open(id)).status, 410);
});
test('disconnect removes both edges and prevents replay reconnection', async () => {
  const id = await create();
  await open(id);
  await call(`/requests/${id}/accept`, 'receiver');
  assert.equal((await call('/requests/receiver/disconnect', 'sender')).status, 200);
  assert.equal(db.data.has('friendUsers/receiver/friends/sender'), false);
  assert.equal(db.data.has('friendUsers/sender/friends/receiver'), false);
  assert.equal((await call(`/requests/${id}/accept`, 'receiver')).status, 409);
});
test('daily limit is enforced and retries do not bypass it', async () => {
  for (let i = 0; i < 20; i++)
    assert.equal(
      (
        await call('/requests/share', 'sender', {
          ...draft,
          submissionId: String(i).padStart(8, '0') + '-1111-4111-8111-111111111111',
        })
      ).status,
      201,
    );
  assert.equal((await call('/requests/share', 'sender', draft)).status, 429);
});
test('old automated delivery endpoints are disabled', async () => {
  assert.equal((await call('/requests/alimtalk', 'sender', draft)).status, 404);
  assert.equal((await call('/requests', 'sender', draft)).status, 404);
});
