const express = require('express');
const nodemailer = require('nodemailer');
const cors = require('cors');
const crypto = require('crypto');
const os = require('os');

const app = express();
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

const PORT = process.env.PORT || 3000;
const SENDER_EMAIL = process.env.SMTP_USER || 'sahayakrequest@gmail.com';
const SENDER_PASS = process.env.SMTP_PASS || 'qboc xxdt lzor dytr';
const ADMIN_EMAIL = 'sahayakadmin65@gmail.com';

// Firebase project config (matches app/google-services.json)
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'sahayak-5c3fe';
const FIREBASE_API_KEY = process.env.FIREBASE_API_KEY || 'AIzaSyDH8rQ9ewVE5cqGkLeKe42MjGohbK2K2cE';
const FIRESTORE_BASE_URL = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents`;

// Configure Nodemailer Transport for Gmail
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: SENDER_EMAIL,
    pass: SENDER_PASS
  }
});

// In-memory cache (backed by Firestore for persistence across restarts)
const applicationsStore = new Map();
const sanghasStore = new Map();

function getLanIp() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

// ─────────────────────────────────────────────────────────────
//  Firestore & Firebase Auth REST Helpers
// ─────────────────────────────────────────────────────────────

function toFirestoreValue(val) {
  if (val === null || val === undefined) return { nullValue: null };
  if (typeof val === 'boolean') return { booleanValue: val };
  if (typeof val === 'number') {
    return Number.isInteger(val) ? { integerValue: String(val) } : { doubleValue: val };
  }
  if (val instanceof Date) return { timestampValue: val.toISOString() };
  if (typeof val === 'string') return { stringValue: val };
  if (Array.isArray(val)) {
    return { arrayValue: { values: val.map(toFirestoreValue) } };
  }
  if (typeof val === 'object') {
    return { mapValue: { fields: toFirestoreFields(val) } };
  }
  return { stringValue: String(val) };
}

function toFirestoreFields(obj) {
  const fields = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v !== undefined) {
      fields[k] = toFirestoreValue(v);
    }
  }
  return fields;
}

function fromFirestoreValue(v) {
  if (!v || typeof v !== 'object') return undefined;
  if ('stringValue' in v) return v.stringValue;
  if ('integerValue' in v) return Number(v.integerValue);
  if ('doubleValue' in v) return Number(v.doubleValue);
  if ('booleanValue' in v) return v.booleanValue;
  if ('timestampValue' in v) return v.timestampValue;
  if ('nullValue' in v) return null;
  if ('arrayValue' in v) return (v.arrayValue.values || []).map(fromFirestoreValue);
  if ('mapValue' in v) return fromFirestoreFields(v.mapValue.fields || {});
  return undefined;
}

function fromFirestoreFields(fields) {
  const out = {};
  for (const [k, v] of Object.entries(fields || {})) {
    out[k] = fromFirestoreValue(v);
  }
  return out;
}

async function getFirestoreDoc(collection, docId) {
  const url = `${FIRESTORE_BASE_URL}/${collection}/${encodeURIComponent(docId)}`;
  const resp = await fetch(url);
  if (resp.status === 404) return null;
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`Firestore GET ${collection}/${docId} failed (${resp.status}): ${text}`);
  }
  const json = await resp.json();
  return fromFirestoreFields(json.fields);
}

async function patchFirestoreDoc(collection, docId, dataObj, updateMaskFields = null) {
  let url = `${FIRESTORE_BASE_URL}/${collection}/${encodeURIComponent(docId)}`;
  if (Array.isArray(updateMaskFields) && updateMaskFields.length > 0) {
    const params = updateMaskFields.map(f => `updateMask.fieldPaths=${encodeURIComponent(f)}`).join('&');
    url += `?${params}`;
  }
  const resp = await fetch(url, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fields: toFirestoreFields(dataObj) })
  });
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`Firestore PATCH ${collection}/${docId} failed (${resp.status}): ${text}`);
  }
  return await resp.json();
}

async function findSanghaDocByApplicationId(appId) {
  const url = `${FIRESTORE_BASE_URL}:runQuery`;
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: 'sanghas' }],
        where: {
          fieldFilter: {
            field: { fieldPath: 'applicationId' },
            op: 'EQUAL',
            value: { stringValue: appId }
          }
        },
        limit: 1
      }
    })
  });
  if (!resp.ok) return null;
  const results = await resp.json();
  for (const row of results) {
    if (row.document && row.document.name) {
      const docId = row.document.name.split('/').pop();
      return { docId, data: fromFirestoreFields(row.document.fields) };
    }
  }
  return null;
}

async function ensureFirebaseAuthUser(email, password) {
  if (!email || !password) {
    return { created: false, uid: null, error: 'Missing email or password' };
  }
  try {
    const signUpUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FIREBASE_API_KEY}`;
    const resp = await fetch(signUpUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, returnSecureToken: true })
    });
    const json = await resp.json();
    if (resp.ok && json.localId) {
      return { created: true, uid: json.localId };
    }
    const errMsg = json?.error?.message || '';
    if (errMsg.includes('EMAIL_EXISTS')) {
      // Account already exists — fetch UID via signInWithPassword if password matches
      const signInUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${FIREBASE_API_KEY}`;
      const siResp = await fetch(signInUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, returnSecureToken: true })
      });
      const siJson = await siResp.json();
      return { created: false, alreadyExists: true, uid: siJson.localId || null };
    }
    return { created: false, uid: null, error: errMsg || 'Auth signUp failed' };
  } catch (err) {
    return { created: false, uid: null, error: err.message };
  }
}

function buildAdminEmailHtml(data, acceptUrl, rejectUrl) {
  const docsListHtml = (data.documents || [])
    .map((doc, idx) => `<li><b>Document ${idx + 1}:</b> ${doc.fileName || 'Attachment'} (${doc.mimeType || 'unknown'})</li>`)
    .join('');

  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        body { font-family: Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; }
        .container { max-width: 650px; background: #ffffff; padding: 30px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin: 0 auto; }
        .header { background: #0D47A1; color: white; padding: 18px 24px; border-radius: 8px 8px 0 0; text-align: center; }
        .title { margin: 0; font-size: 22px; }
        .content { padding: 20px 0; font-size: 14px; color: #333333; line-height: 1.6; }
        .table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        .table th, .table td { border: 1px solid #dddddd; text-align: left; padding: 10px; }
        .table th { background-color: #f2f2f2; width: 35%; color: #444; }
        .actions { margin-top: 30px; text-align: center; }
        .btn { display: inline-block; padding: 14px 28px; margin: 0 10px; font-size: 16px; font-weight: bold; text-decoration: none; border-radius: 6px; color: #ffffff; }
        .btn-accept { background-color: #2E7D32; }
        .btn-reject { background-color: #C62828; }
        .footer { font-size: 12px; color: #777777; margin-top: 25px; text-align: center; border-top: 1px solid #eee; padding-top: 15px; }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h2 class="title">New Sangha Registration Application</h2>
        </div>
        <div class="content">
          <p>A new Sangha has submitted a registration application for verification on <b>Sahayak</b>.</p>

          <h3>Registration Details:</h3>
          <table class="table">
            <tr><th>Application ID</th><td>${data.applicationId}</td></tr>
            <tr><th>Organization Name</th><td>${data.organizationName}</td></tr>
            <tr><th>Registration Number</th><td>${data.registrationNo}</td></tr>
            <tr><th>Contact Person</th><td>${data.contactPerson}</td></tr>
            <tr><th>Official Email</th><td>${data.email}</td></tr>
            <tr><th>Mobile Number</th><td>${data.mobile}</td></tr>
            <tr><th>Locality / Area</th><td>${data.locality || 'N/A'}</td></tr>
            <tr><th>City</th><td>${data.city || 'N/A'}</td></tr>
            <tr><th>District</th><td>${data.district || 'N/A'}</td></tr>
            <tr><th>PIN Code</th><td>${data.pincode || 'N/A'}</td></tr>
            <tr><th>Full Address</th><td>${data.address}</td></tr>
            <tr><th>Google Maps GPS Coords</th><td>${data.latitude && data.longitude ? `<a href="https://www.google.com/maps?q=${data.latitude},${data.longitude}" target="_blank">${data.latitude}° N, ${data.longitude}° E (View on Google Maps)</a>` : 'Pinned Address'}</td></tr>
            <tr><th>Description</th><td>${data.description}</td></tr>
            <tr><th>Username</th><td>${data.username || 'N/A'}</td></tr>
          </table>

          <h3>Uploaded Supporting Documents (${data.documents ? data.documents.length : 0}):</h3>
          <ul>${docsListHtml || '<li>No documents attached</li>'}</ul>

          <p style="margin-top: 20px;">Please review the registration details and attached supporting documents, then select an option below:</p>

          <div class="actions">
            <a href="${acceptUrl}" class="btn btn-accept">ACCEPT</a>
            <a href="${rejectUrl}" class="btn btn-reject">REJECT</a>
          </div>
        </div>
        <div class="footer">
          Sahayak Admin Verification Service &bull; sahayakrequest@gmail.com &bull; sahayakadmin65@gmail.com
        </div>
      </div>
    </body>
    </html>
  `;
}

// GET /api/test-email - Diagnostics endpoint to test Gmail SMTP credentials
app.get('/api/test-email', async (req, res) => {
  if (!SENDER_PASS) {
    return res.status(400).json({
      error: 'Gmail App Password missing',
      instructions: 'Please set environment variable SMTP_PASS=your_gmail_app_password or set SENDER_PASS in server/index.js.'
    });
  }

  try {
    const info = await transporter.sendMail({
      from: SENDER_EMAIL,
      to: ADMIN_EMAIL,
      subject: 'Sahayak Nodemailer Test Connection',
      text: 'Test connection from Sahayak Nodemailer backend server to Admin.'
    });
    return res.json({ success: true, message: 'Test email sent successfully!', info });
  } catch (err) {
    console.error('❌ Gmail SMTP Error:', err);
    return res.status(500).json({
      error: err.message,
      hint: 'Google requires a 16-character App Password generated under Google Account -> Security -> 2-Step Verification -> App Passwords.'
    });
  }
});

// POST /api/notify-admin - Triggered when Sangha submits registration
app.post('/api/notify-admin', async (req, res) => {
  try {
    const data = req.body;
    const { applicationId, reviewToken, organizationName } = data;

    applicationsStore.set(applicationId, { ...data, status: 'PENDING', submittedAt: new Date() });

    let rawHost = req.get('host') || `localhost:${PORT}`;
    if (rawHost.startsWith('10.0.2.2') || rawHost.startsWith('localhost') || rawHost.startsWith('127.0.0.1')) {
      rawHost = `${getLanIp()}:${PORT}`;
    }
    const serverHost = `${req.protocol}://${rawHost}`;
    const acceptUrl = `${serverHost}/api/admin/accept?appId=${encodeURIComponent(applicationId)}&token=${encodeURIComponent(reviewToken || '')}`;
    const rejectUrl = `${serverHost}/api/admin/reject?appId=${encodeURIComponent(applicationId)}&token=${encodeURIComponent(reviewToken || '')}`;

    const attachments = (data.documents || []).map(doc => {
      let contentBuffer = null;
      if (doc.base64Data) {
        const cleanBase64 = doc.base64Data.replace(/^data:[^;]+;base64,/, '');
        contentBuffer = Buffer.from(cleanBase64, 'base64');
      }
      return {
        filename: doc.fileName || 'SupportingDocument.pdf',
        content: contentBuffer || Buffer.from(''),
        contentType: doc.mimeType || 'application/pdf'
      };
    }).filter(att => att.content && att.content.length > 0);

    const mailOptions = {
      from: SENDER_EMAIL,
      to: ADMIN_EMAIL,
      subject: `New Sangha Registration Application: ${organizationName} (${applicationId})`,
      html: buildAdminEmailHtml(data, acceptUrl, rejectUrl),
      attachments: attachments
    };

    let sendSuccess = false;
    let sendError = null;

    if (!SENDER_PASS) {
      console.warn('⚠️ SMTP_PASS environment variable is not set. Email delivery to Gmail requires a Gmail App Password.');
    } else {
      try {
        await transporter.sendMail(mailOptions);
        sendSuccess = true;
        console.log(`✅ Registration email successfully sent from ${SENDER_EMAIL} to ${ADMIN_EMAIL}`);
      } catch (err) {
        sendError = err.message;
        console.error('❌ Failed to send registration email via Gmail SMTP:', err.message);
      }
    }

    return res.status(200).json({
      success: sendSuccess,
      message: sendSuccess ? `Admin notification email sent to ${ADMIN_EMAIL}` : `Application saved, but email could not be delivered: ${sendError || 'Gmail App Password required'}`
    });
  } catch (err) {
    console.error('Error in /api/notify-admin:', err);
    return res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/accept - Admin clicks ACCEPT in email
app.get('/api/admin/accept', async (req, res) => {
  const appId = (req.query.appId || '').trim();
  const token = (req.query.token || '').trim();

  if (!appId) {
    return res.status(400).send('<h2>Missing applicationId</h2>');
  }

  // 1. Load latest application data from Firestore (or fallback to in-memory cache)
  let firestoreApp = null;
  try {
    firestoreApp = await getFirestoreDoc('sangha_applications', appId);
  } catch (err) {
    console.warn('Firestore fetch warning on accept:', err.message);
  }

  let appData = {
    ...(applicationsStore.get(appId) || {}),
    ...(firestoreApp || {}),
    applicationId: appId
  };

  if (firestoreApp && firestoreApp.reviewToken && token && firestoreApp.reviewToken !== token) {
    return res.status(403).send('<h2>Invalid or unauthorized review token.</h2>');
  }

  // 2. Determine / reuse Sangha ID so repeated clicks do not duplicate records
  let sanghaId = appData.sanghaId;
  if (!sanghaId) {
    const existingSangha = await findSanghaDocByApplicationId(appId).catch(() => null);
    if (existingSangha) {
      sanghaId = existingSangha.docId;
    } else {
      sanghaId = 'SNG-' + crypto.randomUUID().slice(0, 6).toUpperCase();
    }
  }

  // 3. Provision Firebase Authentication account automatically using chosen email & password
  const regPassword = appData.tempPassword || (`Sahayak@${(appData.registrationNo || '2026').replace(/[^a-zA-Z0-9]/g, '')}`);
  const authResult = await ensureFirebaseAuthUser(appData.email, regPassword);
  if (authResult.error) {
    console.warn(`⚠️ Firebase Auth provisioning warning for ${appData.email}:`, authResult.error);
  } else {
    console.log(`✅ Firebase Auth ready for ${appData.email} (uid=${authResult.uid || 'existing'})`);
  }

  // 4. Write approved Sangha document into Firestore `sanghas` collection
  const now = new Date();
  const sanghaRecord = {
    sanghaId,
    applicationId: appId,
    name: appData.organizationName || 'Sangha Organization',
    username: appData.username || '',
    email: appData.email || '',
    mobile: appData.mobile || '',
    registrationNo: appData.registrationNo || '',
    contactPerson: appData.contactPerson || '',
    district: appData.district || '',
    locality: appData.locality || '',
    address: appData.address || '',
    description: appData.description || '',
    status: 'APPROVED',
    passwordSet: true,
    createdAt: now
  };
  if (authResult.uid) {
    sanghaRecord.authUid = authResult.uid;
  }

  let firestoreSyncOk = false;
  let firestoreSyncError = null;
  try {
    await patchFirestoreDoc('sanghas', sanghaId, sanghaRecord);
    await patchFirestoreDoc(
      'sangha_applications',
      appId,
      {
        status: 'APPROVED',
        sanghaId,
        adminRemarks: 'Approved by Sahayak Admin via email.',
        reviewedBy: ADMIN_EMAIL,
        reviewedAt: now,
        updatedAt: now,
        registrationTokenUsed: true
      },
      ['status', 'sanghaId', 'adminRemarks', 'reviewedBy', 'reviewedAt', 'updatedAt', 'registrationTokenUsed']
    );
    firestoreSyncOk = true;
    console.log(`✅ Firestore updated: ${appId} -> APPROVED, sanghas/${sanghaId} created.`);
  } catch (err) {
    firestoreSyncError = err.message;
    console.error('❌ Firestore update failed on accept:', err.message);
  }

  appData.status = 'APPROVED';
  appData.sanghaId = sanghaId;
  appData.approvedAt = now;
  applicationsStore.set(appId, appData);
  sanghasStore.set(sanghaId, sanghaRecord);

  // 5. Email approval confirmation to the Sangha member
  if (appData.email && SENDER_PASS) {
    const approvalMailOptions = {
      from: SENDER_EMAIL,
      to: appData.email,
      subject: `Sangha Registration Approved - Welcome to Sahayak!`,
      html: `
        <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
          <h2 style="color: #2E7D32;">Registration Approved!</h2>
          <p>Dear <b>${appData.organizationName || 'Sangha Partner'}</b>,</p>
          <p>Your Sangha registration application (<b>ID: ${appId}</b>) has been <b>ACCEPTED</b> by the Sahayak Admin team.</p>
          <p>Your account is now <b>ACTIVE</b> and automatically registered in the Sahayak platform (Sangha ID: <b>${sanghaId}</b>).</p>
          <p>You can now sign in directly on the Sahayak mobile app using your registered credentials:</p>
          <ul>
            <li><b>Username:</b> ${appData.username || 'Your registered username'}</li>
          </ul>
          <p>Thank you for partnering with Sahayak to help society!</p>
          <hr style="margin-top: 20px; border: 0; border-top: 1px solid #eee;">
          <p style="font-size: 12px; color: #777;">Sahayak Administration Team &bull; sahayakadmin65@gmail.com</p>
        </div>
      `
    };
    await transporter.sendMail(approvalMailOptions).catch(err => console.warn('Approval mail send notice:', err.message));
  }

  return res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>Registration Accepted</title>
      <style>
        body { font-family: Arial, sans-serif; background: #e8f5e9; padding: 40px; text-align: center; }
        .card { background: white; padding: 30px; border-radius: 12px; display: inline-block; box-shadow: 0 4px 10px rgba(0,0,0,0.1); max-width: 520px; }
        h2 { color: #2e7d32; }
        .meta { text-align: left; background: #f8f9fa; padding: 12px 16px; border-radius: 8px; margin: 16px 0; font-size: 14px; }
      </style>
    </head>
    <body>
      <div class="card">
        <h2>✓ Sangha Registration Accepted</h2>
        <p>Sangha <b>${appData.organizationName || appId}</b> has been approved and registered in Sahayak.</p>
        <div class="meta">
          <div><b>Application ID:</b> ${appId}</div>
          <div><b>Sangha ID:</b> ${sanghaId}</div>
          <div><b>Username:</b> ${appData.username || 'N/A'}</div>
          <div><b>Firestore Sync:</b> ${firestoreSyncOk ? 'Synced ✅' : 'Failed ❌ (' + firestoreSyncError + ')'}</div>
        </div>
        <p>An approval confirmation email has been sent to <b>${appData.email || 'the registered email'}</b>.</p>
        <p>The Sangha can now log in immediately on the mobile app.</p>
      </div>
    </body>
    </html>
  `);
});

// GET /api/admin/reject - Admin clicks REJECT in email
app.get('/api/admin/reject', async (req, res) => {
  const appId = (req.query.appId || '').trim();
  const token = (req.query.token || '').trim();

  if (!appId) {
    return res.status(400).send('<h2>Missing applicationId</h2>');
  }

  let firestoreApp = null;
  try {
    firestoreApp = await getFirestoreDoc('sangha_applications', appId);
  } catch (err) {
    console.warn('Firestore fetch warning on reject:', err.message);
  }

  let appData = {
    ...(applicationsStore.get(appId) || {}),
    ...(firestoreApp || {}),
    applicationId: appId
  };

  if (firestoreApp && firestoreApp.reviewToken && token && firestoreApp.reviewToken !== token) {
    return res.status(403).send('<h2>Invalid or unauthorized review token.</h2>');
  }

  const now = new Date();
  let firestoreSyncOk = false;
  try {
    await patchFirestoreDoc(
      'sangha_applications',
      appId,
      {
        status: 'REJECTED',
        adminRemarks: 'Rejected by Sahayak Admin via email.',
        reviewedBy: ADMIN_EMAIL,
        reviewedAt: now,
        updatedAt: now
      },
      ['status', 'adminRemarks', 'reviewedBy', 'reviewedAt', 'updatedAt']
    );
    const existingSangha = await findSanghaDocByApplicationId(appId).catch(() => null);
    if (existingSangha) {
      await patchFirestoreDoc('sanghas', existingSangha.docId, { status: 'REJECTED' }, ['status']);
    }
    firestoreSyncOk = true;
    console.log(`✅ Firestore updated: ${appId} -> REJECTED`);
  } catch (err) {
    console.error('❌ Firestore update failed on reject:', err.message);
  }

  appData.status = 'REJECTED';
  appData.rejectedAt = now;
  applicationsStore.set(appId, appData);

  if (appData.email && SENDER_PASS) {
    const rejectionMailOptions = {
      from: SENDER_EMAIL,
      to: appData.email,
      subject: `Sangha Registration Application Status - Sahayak`,
      html: `
        <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
          <h2 style="color: #C62828;">Registration Application Rejected</h2>
          <p>Dear <b>${appData.organizationName || 'Sangha Partner'}</b>,</p>
          <p>We regret to inform you that your Sangha registration application (<b>ID: ${appId}</b>) has been <b>REJECTED</b> by Sahayak Admin after review.</p>
          <p>Consequently, your application is cancelled and login access to the Sahayak application is restricted.</p>
          <p>If you believe this is in error or require further clarification, please contact <b>sahayakadmin65@gmail.com</b>.</p>
          <hr style="margin-top: 20px; border: 0; border-top: 1px solid #eee;">
          <p style="font-size: 12px; color: #777;">Sahayak Administration Team &bull; sahayakadmin65@gmail.com</p>
        </div>
      `
    };
    await transporter.sendMail(rejectionMailOptions).catch(err => console.warn('Rejection mail send notice:', err.message));
  }

  return res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>Registration Rejected</title>
      <style>
        body { font-family: Arial, sans-serif; background: #ffebee; padding: 40px; text-align: center; }
        .card { background: white; padding: 30px; border-radius: 12px; display: inline-block; box-shadow: 0 4px 10px rgba(0,0,0,0.1); max-width: 500px; }
        h2 { color: #c62828; }
      </style>
    </head>
    <body>
      <div class="card">
        <h2>✕ Sangha Registration Rejected</h2>
        <p>Application <b>${appId}</b> (${appData.organizationName || 'Sangha'}) has been set to <b>REJECTED</b>.</p>
        <p>Firestore Status: ${firestoreSyncOk ? 'Updated ✅' : 'Warning ⚠️'}</p>
        <p>A rejection email notification has been sent to <b>${appData.email || 'the registered email'}</b>.</p>
        <p>Login access for this Sangha is now restricted.</p>
      </div>
    </body>
    </html>
  `);
});

// POST /api/notify-sangha-decision - Triggered by Admin Review Activity in app
app.post('/api/notify-sangha-decision', async (req, res) => {
  try {
    const { applicationId, organizationName, toEmail, decision, remarks } = req.body;
    const isApproved = decision && decision.toUpperCase() === 'APPROVED';

    const subject = isApproved
      ? `Sangha Registration Approved - Welcome to Sahayak!`
      : `Sangha Registration Application Status - Sahayak`;

    const html = isApproved ? `
      <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
        <h2 style="color: #2E7D32;">Registration Approved!</h2>
        <p>Dear <b>${organizationName || 'Sangha Partner'}</b>,</p>
        <p>Your Sangha registration application (ID: <b>${applicationId}</b>) has been <b>ACCEPTED</b> by Sahayak Admin.</p>
        <p>Your account is now active. You can log in to the Sahayak application using your registered username and password.</p>
        <hr style="margin-top: 20px; border: 0; border-top: 1px solid #eee;">
        <p style="font-size: 12px; color: #777;">Sahayak Administration Team &bull; sahayakadmin65@gmail.com</p>
      </div>
    ` : `
      <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
        <h2 style="color: #C62828;">Registration Application Rejected</h2>
        <p>Dear <b>${organizationName || 'Sangha Partner'}</b>,</p>
        <p>Your Sangha registration application (ID: <b>${applicationId}</b>) has been <b>REJECTED</b>.</p>
        <p><b>Reason:</b> ${remarks || 'Registration criteria not met.'}</p>
        <hr style="margin-top: 20px; border: 0; border-top: 1px solid #eee;">
        <p style="font-size: 12px; color: #777;">Sahayak Administration Team &bull; sahayakadmin65@gmail.com</p>
      </div>
    `;

    if (SENDER_PASS) {
      await transporter.sendMail({
        from: SENDER_EMAIL,
        to: toEmail,
        subject: subject,
        html: html
      }).catch(err => console.warn('Decision mail notice:', err.message));
    }

    return res.status(200).json({ success: true, message: 'Decision email processed.' });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`\n======================================================`);
  console.log(`🚀 Sahayak Email Server listening on port ${PORT}`);
  console.log(`🌐 LAN Address: http://${getLanIp()}:${PORT}`);
  console.log(`📧 Sender Email: ${SENDER_EMAIL}`);
  console.log(`🔥 Firebase Project: ${FIREBASE_PROJECT_ID}`);
  console.log(`⚙️  SMTP Pass status: ${SENDER_PASS ? 'CONFIGURED ✅' : 'NOT SET ⚠️'}`);
  console.log(`======================================================\n`);
});
