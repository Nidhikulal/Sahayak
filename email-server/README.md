# Sahayak Nodemailer Email Server

Sends real emails via Gmail + Nodemailer for the Sangha Verification Workflow:
- **System Sender (`SMTP_USER`)**: `sahayakrequest@gmail.com`
- **Admin Inbox (`ADMIN_EMAIL`)**: `sahayakadmin65@gmail.com`

## Workflow
1. When a Sangha submits registration in the app, `sahayakrequest@gmail.com` sends a verification email (with all organization details, uploaded documents, and **Accept** / **Reject** buttons) to `sahayakadmin65@gmail.com`.
2. When the admin clicks **Accept**, the server automatically creates the Sangha's Firebase Auth login account, adds the Sangha to Firestore (`sanghas`), and emails a login link to the Sangha from `sahayakrequest@gmail.com`.
3. When the admin clicks **Reject**, the server marks the application `REJECTED` in Firestore and emails the rejection reason to the Sangha from `sahayakrequest@gmail.com`.

## Setup (Free Gmail SMTP)
1. Open Google Account settings for **`sahayakrequest@gmail.com`**:
   - Enable **2-Step Verification**.
   - Go to **Security → App passwords**, generate an App Password for **Mail**.
2. Edit `.env` and replace `REPLACE_WITH_GMAIL_APP_PASSWORD` with that 16-character App Password.
3. Install & start:
   ```bash
   npm install
   npm start
   ```
   The server runs on port `3000` (`http://10.0.2.2:3000` from the Android Emulator, `http://localhost:3000` from the admin's PC browser).
