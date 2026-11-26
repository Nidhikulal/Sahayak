require('dotenv').config();
const express = require('express');
const cors = require('cors');
const nodemailer = require('nodemailer');
const https = require('https');
const http = require('http');

const app = express();
app.use(cors());
app.use(express.json({ limit: '5mb' }));

const PORT = process.env.PORT || 3000;
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || 'sahayakadmin65@gmail.com';
const SMTP_USER = process.env.SMTP_USER || 'sahayakrequest@gmail.com';
const SMTP_PASS = process.env.SMTP_PASS || '';
const PUBLIC_BASE_URL = (process.env.PUBLIC_BASE_URL || `http://localhost:${PORT}`).replace(/\/$/, '');
const FIREBASE_API_KEY = process.env.FIREBASE_API_KEY || 'AIzaSyDH8rQ9ewVE5cqGkLeKe42MjGohbK2K2cE';
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'sahayak-5c3fe';

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: { user: SMTP_USER, pass: SMTP_PASS },
});

function isConfigured() {
  return SMTP_PASS && SMTP_PASS !== 'REPLACE_WITH_GMAIL_APP_PASSWORD' && SMTP_PASS !== 'your_16_char_gmail_app_password';
}

function escapeHtml(str = '') {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ── Firestore REST helpers ────────────────────────────────────────────────────

function firestoreGet(collection, docId) {
  return new Promise((resolve, reject) => {
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${collection}/${docId}`;
    https.get(url, (res) => {
      let body = '';
      res.on('data', d => body += d);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          if (json.error) return reject(new Error(json.error.message));
          resolve(json);
        } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

function firestoreGetField(firestoreDoc, field) {
  const fields = firestoreDoc.fields || {};
  const f = fields[field];
  if (!f) return '';
  return f.stringValue || f.integerValue || f.booleanValue || '';
}

function firestorePatch(collection, docId, fieldsObj) {
  return new Promise((resolve, reject) => {
    const fields = {};
    for (const [k, v] of Object.entries(fieldsObj)) {
      if (typeof v === 'string') fields[k] = { stringValue: v };
      else if (typeof v === 'boolean') fields[k] = { booleanValue: v };
      else if (typeof v === 'number') fields[k] = { integerValue: String(v) };
    }
    const body = JSON.stringify({ fields });
    const fieldMask = Object.keys(fieldsObj).join(',');
    const urlPath = `/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${collection}/${docId}?updateMask.fieldPaths=${encodeURIComponent(fieldMask)}`;
    const options = {
      hostname: 'firestore.googleapis.com',
      path: urlPath,
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) },
    };
    const req = https.request(options, (res) => {
      let respBody = '';
      res.on('data', d => respBody += d);
      res.on('end', () => {
        try {
          const json = JSON.parse(respBody);
          if (json.error) return reject(new Error(json.error.message));
          resolve(json);
        } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

function firestoreCreate(collection, docId, fieldsObj) {
  return new Promise((resolve, reject) => {
    const fields = {};
    for (const [k, v] of Object.entries(fieldsObj)) {
      if (typeof v === 'string') fields[k] = { stringValue: v };
      else if (typeof v === 'boolean') fields[k] = { booleanValue: v };
      else if (typeof v === 'number') fields[k] = { integerValue: String(v) };
    }
    const body = JSON.stringify({ fields });
    const urlPath = `/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${collection}?documentId=${docId}`;
    const options = {
      hostname: 'firestore.googleapis.com',
      path: urlPath,
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) },
    };
    const req = https.request(options, (res) => {
      let respBody = '';
      res.on('data', d => respBody += d);
      res.on('end', () => {
        try {
          const json = JSON.parse(respBody);
          if (json.error) return reject(new Error(json.error.message));
          resolve(json);
        } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

function firebaseSignUp(email, password) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({ email, password, returnSecureToken: true });
    const options = {
      hostname: 'identitytoolkit.googleapis.com',
      path: `/v1/accounts:signUp?key=${FIREBASE_API_KEY}`,
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) },
    };
    const req = https.request(options, (res) => {
      let respBody = '';
      res.on('data', d => respBody += d);
      res.on('end', () => {
        try {
          const json = JSON.parse(respBody);
          if (json.error) return reject(new Error(json.error.message));
          resolve(json); // { localId, idToken, email, ... }
        } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function randomId(prefix) {
  return prefix + '-' + Math.random().toString(36).substr(2, 6).toUpperCase();
}

function extractFirestoreDocuments(firestoreDoc) {
  const docsField = firestoreDoc?.fields?.documents?.arrayValue?.values;
  if (!Array.isArray(docsField)) return [];
  const attachments = [];
  for (const item of docsField) {
    const f = item?.mapValue?.fields;
    if (!f) continue;
    const fileName = f.fileName?.stringValue || 'document';
    const mimeType = f.mimeType?.stringValue || 'application/octet-stream';
    const base64Data = f.base64Data?.stringValue || '';
    if (base64Data) {
      attachments.push({
        filename: fileName,
        content: Buffer.from(base64Data, 'base64'),
        contentType: mimeType,
      });
    }
  }
  return attachments;
}

async function sendMail(to, subject, html, attachments = []) {
  if (!isConfigured()) throw new Error('SMTP not configured. Add Gmail App Password for sahayakrequest@gmail.com in email-server/.env');
  return transporter.sendMail({
    from: `"Sahayak System" <${SMTP_USER}>`,
    to,
    subject,
    html,
    attachments,
  });
}

// ── Health ────────────────────────────────────────────────────────────────────

app.get('/health', (_req, res) => {
  res.json({ ok: true, systemEmail: SMTP_USER, adminEmail: ADMIN_EMAIL, smtpConfigured: isConfigured(), publicBaseUrl: PUBLIC_BASE_URL });
});

// ── 1. Notify admin when Sangha submits application ───────────────────────────

app.post('/api/notify-admin', async (req, res) => {
  const {
    applicationId, organizationName, registrationNo,
    email, mobile, contactPerson, address, description,
    district: bodyDistrict, locality: bodyLocality, documentCount, reviewToken,
    isResubmission, resubmissionNote,
  } = req.body || {};

  if (!applicationId || !organizationName || !reviewToken) {
    return res.status(400).json({ ok: false, error: 'Missing required fields.' });
  }

  // Fetch district, locality, chosen username, and uploaded documents from Firestore
  let district = bodyDistrict || '';
  let locality = bodyLocality || '';
  let username = '';
  let attachments = [];
  try {
    const appDoc = await firestoreGet('sangha_applications', applicationId);
    district = district || firestoreGetField(appDoc, 'district');
    locality = locality || firestoreGetField(appDoc, 'locality');
    username = firestoreGetField(appDoc, 'username');
    attachments = extractFirestoreDocuments(appDoc);
  } catch (e) {
    console.warn('Could not fetch extra details/attachments from Firestore:', e.message);
  }

  const acceptUrl = `${PUBLIC_BASE_URL}/api/admin/accept?appId=${encodeURIComponent(applicationId)}&token=${encodeURIComponent(reviewToken)}`;
  const rejectUrl = `${PUBLIC_BASE_URL}/api/admin/reject?appId=${encodeURIComponent(applicationId)}&token=${encodeURIComponent(reviewToken)}`;
  const subjectPrefix = isResubmission ? '[Resubmitted] ' : '';

  const html = `
  <div style="font-family:Arial,sans-serif;max-width:640px;margin:0 auto;border:1px solid #ddd;border-radius:12px;overflow:hidden;">
    <div style="background:#2E7D32;color:#fff;padding:22px 28px;">
      <h2 style="margin:0;">Sahayak — Sangha Verification ${isResubmission ? 'Resubmission' : 'Request'}</h2>
      <p style="margin:6px 0 0;opacity:.9;">Application ID: <strong>${escapeHtml(applicationId)}</strong></p>
    </div>
    <div style="padding:24px;color:#222;line-height:1.6;">
      <p>Hello Admin,</p>
      <p>${isResubmission ? 'A Sangha has resubmitted additional details:' : 'A new Sangha has registered and requires verification before access is granted:'}</p>
      ${resubmissionNote ? `<div style="background:#FFF8E1;border-left:4px solid #FFA000;padding:10px 14px;margin:12px 0;"><strong>Sangha Note:</strong> ${escapeHtml(resubmissionNote)}</div>` : ''}
      <table style="width:100%;border-collapse:collapse;margin:16px 0;">
        <tr><td style="padding:6px 0;color:#666;width:160px;">Organization:</td><td><strong>${escapeHtml(organizationName)}</strong></td></tr>
        <tr><td style="padding:6px 0;color:#666;">Registration No.:</td><td>${escapeHtml(registrationNo)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Contact Person:</td><td>${escapeHtml(contactPerson)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Official Email:</td><td>${escapeHtml(email)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Mobile:</td><td>${escapeHtml(mobile)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Requested Username:</td><td><strong>${escapeHtml(username || 'N/A')}</strong></td></tr>
        <tr><td style="padding:6px 0;color:#666;">District:</td><td>${escapeHtml(district)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Locality:</td><td>${escapeHtml(locality)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Address:</td><td>${escapeHtml(address)}</td></tr>
        <tr><td style="padding:6px 0;color:#666;">Documents:</td><td><strong>${attachments.length || Number(documentCount || 0)} file(s) attached</strong></td></tr>
      </table>
      <p><strong>Activities:</strong><br/>${escapeHtml(description)}</p>

      <p style="margin-top:28px;text-align:center;font-size:15px;"><strong>Take Action:</strong></p>
      <div style="text-align:center;margin:16px 0 28px;">
        <a href="${acceptUrl}"
           style="background:#2E7D32;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:bold;display:inline-block;margin:0 10px;font-size:15px;">
          ✅ Accept Application
        </a>
        <a href="${rejectUrl}"
           style="background:#C62828;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:bold;display:inline-block;margin:0 10px;font-size:15px;">
          ❌ Reject Application
        </a>
      </div>
      <p style="font-size:12px;color:#888;text-align:center;">
        Sent from <strong>${escapeHtml(SMTP_USER)}</strong> to <strong>${escapeHtml(ADMIN_EMAIL)}</strong>.<br/>
        Clicking a button above is <strong>instant and automatic</strong> — the Sangha will be notified by email immediately.
      </p>
    </div>
  </div>`;

  if (!isConfigured()) {
    console.warn('[SMTP NOT CONFIGURED] Would email admin from', SMTP_USER, 'to', ADMIN_EMAIL, 'for', applicationId);
    return res.status(503).json({ ok: false, error: 'Configure SMTP_PASS for sahayakrequest@gmail.com in email-server/.env to send emails.' });
  }

  try {
    const info = await sendMail(
      ADMIN_EMAIL,
      `${subjectPrefix}Sangha Verification Application (${applicationId}) — ${organizationName}`,
      html,
      attachments
    );
    console.log(`Admin notified (${SMTP_USER} -> ${ADMIN_EMAIL}):`, info.messageId);
    return res.json({ ok: true, messageId: info.messageId });
  } catch (err) {
    console.error('Admin email failed:', err);
    return res.status(500).json({ ok: false, error: err.message });
  }
});

// ── 2. Admin clicks ACCEPT in email (fully automatic) ─────────────────────────

app.get('/api/admin/accept', async (req, res) => {
  const appId = String(req.query.appId || '').trim();
  const token = String(req.query.token || '').trim();

  if (!appId || !token) {
    return res.status(400).send(htmlResult('error', 'Missing application ID or token.'));
  }

  try {
    // 1. Load application from Firestore
    const appDoc = await firestoreGet('sangha_applications', appId);
    const fields = appDoc.fields || {};

    const storedToken = firestoreGetField(appDoc, 'reviewToken');
    const status = firestoreGetField(appDoc, 'status');
    const orgName = firestoreGetField(appDoc, 'organizationName');
    const email = firestoreGetField(appDoc, 'email');
    const mobile = firestoreGetField(appDoc, 'mobile');
    const registrationNo = firestoreGetField(appDoc, 'registrationNo');
    const contactPerson = firestoreGetField(appDoc, 'contactPerson');
    const district = firestoreGetField(appDoc, 'district');
    const locality = firestoreGetField(appDoc, 'locality');
    const address = firestoreGetField(appDoc, 'address');
    const description = firestoreGetField(appDoc, 'description');
    const username = firestoreGetField(appDoc, 'username');
    const tempPassword = firestoreGetField(appDoc, 'tempPassword');

    if (storedToken !== token) {
      return res.send(htmlResult('error', 'Invalid or expired review token. This link may have already been used.'));
    }
    if (status === 'APPROVED' || status === 'COMPLETED') {
      return res.send(htmlResult('info', `This application (${appId}) has already been approved.`));
    }
    if (status === 'REJECTED') {
      return res.send(htmlResult('error', `This application (${appId}) was already rejected.`));
    }

    // 2. Create Firebase Auth user for the Sangha
    let authUid = '';
    try {
      const authResult = await firebaseSignUp(email, tempPassword);
      authUid = authResult.localId || '';
      console.log('Firebase Auth user created:', authUid);
    } catch (authErr) {
      // If email already in use, still continue — allow the existing account
      console.warn('Firebase Auth user creation warning:', authErr.message);
      if (!authErr.message.includes('EMAIL_EXISTS')) {
        return res.send(htmlResult('error', 'Failed to create login account: ' + authErr.message));
      }
    }

    // 3. Create Sangha document in Firestore
    const sanghaId = randomId('SNG');
    await firestoreCreate('sanghas', sanghaId, {
      sanghaId,
      applicationId: appId,
      authUid,
      name: orgName,
      username,
      email,
      emailLower: email.toLowerCase(),
      mobile,
      registrationNo,
      regNoUpper: registrationNo.toUpperCase(),
      contactPerson,
      district,
      locality,
      city: locality,
      address,
      description,
      status: 'APPROVED',
      passwordSet: 'true',
      createdAt: new Date().toISOString(),
    });
    console.log('Sangha document created:', sanghaId);

    // 4. Update application status to APPROVED + clear tempPassword for security
    await firestorePatch('sangha_applications', appId, {
      status: 'APPROVED',
      reviewedAt: new Date().toISOString(),
      sanghaId,
      authUid,
      tempPassword: '',  // clear sensitive field
      adminAction: 'ACCEPTED',
    });

    // 5. Email the Sangha with login link
    const loginLink = `${PUBLIC_BASE_URL}/open/login`;
    const html = `
    <div style="font-family:Arial,sans-serif;max-width:620px;margin:0 auto;border:1px solid #ddd;border-radius:12px;overflow:hidden;">
      <div style="background:#2E7D32;color:#fff;padding:22px 28px;">
        <h2 style="margin:0;">🎉 Application Approved!</h2>
        <p style="margin:6px 0 0;opacity:.9;">Application ID: <strong>${escapeHtml(appId)}</strong></p>
      </div>
      <div style="padding:24px;color:#222;line-height:1.6;">
        <p>Dear <strong>${escapeHtml(orgName)}</strong>,</p>
        <p>Congratulations! Your Sangha verification application has been <strong>approved</strong> by the Sahayak administration.</p>
        <p>Your account has been created. Use the credentials below to sign in:</p>
        <table style="width:100%;border-collapse:collapse;margin:16px 0;background:#F1F8E9;border-radius:8px;padding:12px;">
          <tr><td style="padding:8px 12px;color:#555;">Username:</td><td style="padding:8px 12px;"><strong>${escapeHtml(username)}</strong></td></tr>
          <tr><td style="padding:8px 12px;color:#555;">Password:</td><td style="padding:8px 12px;"><em>The password you created during registration</em></td></tr>
          <tr><td style="padding:8px 12px;color:#555;">Sangha ID:</td><td style="padding:8px 12px;">${escapeHtml(sanghaId)}</td></tr>
        </table>
        <div style="text-align:center;margin:28px 0;">
          <a href="${loginLink}" style="background:#2E7D32;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:bold;font-size:15px;display:inline-block;">
            🔑 Sign In to Sahayak
          </a>
        </div>
        <p style="font-size:12px;color:#888;">
          Open the Sahayak app → Sangha Portal → Sign In with your username and password.
        </p>
      </div>
    </div>`;

    try {
      await sendMail(email, `Sahayak Sangha Account Approved — Welcome, ${orgName}!`, html);
      console.log('Approval email sent to:', email);
    } catch (mailErr) {
      console.error('Approval email error:', mailErr.message);
    }

    return res.send(htmlResult('success',
      `✅ Application <strong>${escapeHtml(appId)}</strong> for <strong>${escapeHtml(orgName)}</strong> has been <strong>Approved</strong>!<br/><br/>
       The Sangha account has been created and a login email has been sent to <strong>${escapeHtml(email)}</strong>.`));

  } catch (err) {
    console.error('Accept error:', err);
    return res.send(htmlResult('error', 'An error occurred: ' + err.message));
  }
});

// ── 3. Admin clicks REJECT in email (fully automatic) ─────────────────────────

app.get('/api/admin/reject', async (req, res) => {
  const appId = String(req.query.appId || '').trim();
  const token = String(req.query.token || '').trim();
  const reason = String(req.query.reason || '').trim() || 'Your application did not meet our verification requirements.';

  if (!appId || !token) {
    return res.status(400).send(htmlResult('error', 'Missing application ID or token.'));
  }

  // Show a rejection form first so admin can optionally add a reason, then confirm
  if (!req.query.confirmed) {
    return res.send(`<!DOCTYPE html>
    <html><head><meta charset="utf-8"><title>Reject Application</title>
    <style>body{font-family:Arial,sans-serif;background:#fafafa;margin:0;padding:24px;color:#222;}
    .card{max-width:500px;margin:40px auto;background:#fff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,.08);}
    h2{color:#C62828;margin-top:0;} textarea{width:100%;box-sizing:border-box;padding:10px;border-radius:8px;border:1px solid #ccc;font-size:14px;margin-top:8px;}
    .btn{display:inline-block;margin-top:16px;padding:12px 28px;border-radius:8px;font-weight:bold;text-decoration:none;color:#fff;border:none;cursor:pointer;font-size:15px;}
    .btn-reject{background:#C62828;} .btn-cancel{background:#888;margin-left:12px;}
    </style></head><body>
    <div class="card">
      <h2>❌ Reject Application</h2>
      <p>Application ID: <strong>${escapeHtml(appId)}</strong></p>
      <form method="GET" action="/api/admin/reject">
        <input type="hidden" name="appId" value="${escapeHtml(appId)}">
        <input type="hidden" name="token" value="${escapeHtml(token)}">
        <input type="hidden" name="confirmed" value="1">
        <label><strong>Reason for Rejection</strong> (will be emailed to Sangha):</label>
        <textarea name="reason" rows="4" placeholder="e.g. Documents could not be verified...">${escapeHtml(reason)}</textarea>
        <br/>
        <button type="submit" class="btn btn-reject">Confirm Rejection &amp; Email Sangha</button>
      </form>
    </div></body></html>`);
  }

  try {
    const appDoc = await firestoreGet('sangha_applications', appId);
    const storedToken = firestoreGetField(appDoc, 'reviewToken');
    const status = firestoreGetField(appDoc, 'status');
    const orgName = firestoreGetField(appDoc, 'organizationName');
    const email = firestoreGetField(appDoc, 'email');

    if (storedToken !== token) {
      return res.send(htmlResult('error', 'Invalid or expired review token.'));
    }
    if (status === 'REJECTED') {
      return res.send(htmlResult('info', `This application (${appId}) has already been rejected.`));
    }
    if (status === 'APPROVED' || status === 'COMPLETED') {
      return res.send(htmlResult('error', `This application (${appId}) was already approved and cannot be rejected.`));
    }

    // Update status in Firestore
    await firestorePatch('sangha_applications', appId, {
      status: 'REJECTED',
      adminRemarks: reason,
      reviewedAt: new Date().toISOString(),
      tempPassword: '', // clear sensitive field
      adminAction: 'REJECTED',
    });

    // Email rejection to Sangha
    const html = `
    <div style="font-family:Arial,sans-serif;max-width:620px;margin:0 auto;border:1px solid #ddd;border-radius:12px;overflow:hidden;">
      <div style="background:#C62828;color:#fff;padding:22px 28px;">
        <h2 style="margin:0;">Application Not Approved</h2>
        <p style="margin:6px 0 0;opacity:.9;">Application ID: <strong>${escapeHtml(appId)}</strong></p>
      </div>
      <div style="padding:24px;color:#222;line-height:1.6;">
        <p>Dear <strong>${escapeHtml(orgName)}</strong>,</p>
        <p>We regret to inform you that your Sangha verification application was <strong>rejected</strong> after administrative review.</p>
        <div style="background:#FFEBEE;border-left:4px solid #C62828;padding:12px 16px;margin:16px 0;">
          <strong>Reason for Rejection:</strong><br/>
          ${escapeHtml(reason)}
        </div>
        <p>If you believe this was an error or wish to provide additional information, please contact us at
          <a href="mailto:${escapeHtml(ADMIN_EMAIL)}">${escapeHtml(ADMIN_EMAIL)}</a>.
        </p>
      </div>
    </div>`;

    try {
      await sendMail(email, `Sahayak Sangha Application Update (${appId}) — Not Approved`, html);
      console.log('Rejection email sent to:', email);
    } catch (mailErr) {
      console.error('Rejection email error:', mailErr.message);
    }

    return res.send(htmlResult('info',
      `Application <strong>${escapeHtml(appId)}</strong> for <strong>${escapeHtml(orgName)}</strong> has been <strong>Rejected</strong>.<br/><br/>
       A rejection email has been sent to <strong>${escapeHtml(email)}</strong>.`));

  } catch (err) {
    console.error('Reject error:', err);
    return res.send(htmlResult('error', 'An error occurred: ' + err.message));
  }
});

// ── 4. Bridge page: email login link ──────────────────────────────────────────

app.get('/open/login', (req, res) => {
  const deepLink = 'sahayak://sangha/login';
  res.send(`<!DOCTYPE html>
  <html><head><meta charset="utf-8"><title>Sahayak Sign In</title>
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <style>body{font-family:Arial,sans-serif;background:#F5F7F5;margin:0;padding:24px;color:#222;}
  .card{max-width:480px;margin:40px auto;background:#fff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,.08);text-align:center;}
  .btn{display:inline-block;margin-top:18px;background:#2E7D32;color:#fff;text-decoration:none;padding:14px 28px;border-radius:10px;font-weight:bold;font-size:15px;}
  </style></head><body>
  <div class="card">
    <h2 style="color:#2E7D32;margin-top:0;">Sahayak Sangha Portal</h2>
    <p>Tap below to open the Sahayak app and sign in with your credentials.</p>
    <a class="btn" href="${deepLink}">🔑 Open Sahayak App</a>
    <p style="margin-top:20px;font-size:13px;color:#888;">
      Or open Sahayak → Sangha Portal → Sign In
    </p>
  </div>
  <script>setTimeout(function(){ window.location.href='${deepLink}'; }, 300);</script>
  </body></html>`);
});

// ── Helper: render admin action result page ───────────────────────────────────

function htmlResult(type, message) {
  const colors = { success: '#2E7D32', error: '#C62828', info: '#1565C0' };
  const icons = { success: '✅', error: '❌', info: 'ℹ️' };
  const bg = { success: '#E8F5E9', error: '#FFEBEE', info: '#E3F2FD' };
  const color = colors[type] || colors.info;
  const icon = icons[type] || '';
  const bgColor = bg[type] || bg.info;

  return `<!DOCTYPE html>
  <html><head><meta charset="utf-8"><title>Sahayak Admin Action</title>
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <style>body{font-family:Arial,sans-serif;background:#fafafa;margin:0;padding:24px;}
  .card{max-width:500px;margin:50px auto;border-radius:16px;padding:32px;box-shadow:0 4px 16px rgba(0,0,0,.08);background:${bgColor};text-align:center;}
  h2{color:${color};} p{font-size:15px;line-height:1.6;color:#333;}
  </style></head><body>
  <div class="card">
    <h2>${icon} Sahayak Admin Action</h2>
    <p>${message}</p>
    <p style="font-size:13px;color:#888;margin-top:24px;">You can close this browser tab.</p>
  </div>
  </body></html>`;
}

// ── Start ─────────────────────────────────────────────────────────────────────

app.listen(PORT, () => {
  console.log(`\nSahayak Nodemailer server running on http://localhost:${PORT}`);
  console.log(`Admin email:   ${ADMIN_EMAIL}`);
  console.log(`SMTP ready:    ${isConfigured()}`);
  console.log(`Public URL:    ${PUBLIC_BASE_URL}`);
  console.log(`Firebase:      ${FIREBASE_PROJECT_ID}\n`);
});
