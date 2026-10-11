const { onRequest } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');
const { createApi } = require('./api.cjs');

initializeApp();
const projectId = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;

exports.friendsApi = onRequest(
  { region: 'asia-northeast3', timeoutSeconds: 60, maxInstances: 5, invoker: 'public' },
  createApi({
    db: getFirestore(),
    auth: getAuth(),
    publicOrigin: `https://${projectId}.web.app`,
  }),
);
