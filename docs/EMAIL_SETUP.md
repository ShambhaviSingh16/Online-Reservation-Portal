# Email on the free Render service

TrackEase supports booking, cancellation, waiting-list/RAC promotion notifications,
and email-based password reset. Email credentials are optional for running the rest
of the application. Without them, recovery codes still work and booking events
remain in the notification outbox.

## Recommended: Brevo Free over HTTPS

Brevo's Free plan currently includes **300 sends per day**, with no credit card
required. Use the Free plan; there is no need to buy credits, add a payment method,
or upgrade for this portfolio demonstration. Limits and sender-approval policies
are controlled by the provider.

Render's free web services block outbound SMTP ports 25, 465, and 587. TrackEase's
Brevo adapter uses its HTTPS email API instead. The optional SMTP adapter is useful
for local testing or a host that permits SMTP connections.

1. Create a free account at [Brevo](https://www.brevo.com/).
2. Complete the account verification steps required by Brevo.
3. Add a sender email address that you own and verify it through Brevo's sender settings.
4. Create an **API key** in the SMTP & API settings. Use an API key, not an SMTP key.
5. Open **Render → TrackEase web service → Environment**.
6. Add the following variables:

   | Key | Value |
   | --- | --- |
   | `MAIL_PROVIDER` | `brevo` |
   | `BREVO_API_KEY` | Your private API key |
   | `MAIL_FROM` | Your verified sender email address |
   | `PUBLIC_BASE_URL` | `https://trackease-njtn.onrender.com` |

7. Save the settings and redeploy the latest code.
8. Register an account using an inbox you control. Request a password-reset link or
   make a demonstration booking, then check the inbox and spam folder.

Do not paste API keys into chat, screenshots, source files, or GitHub. The key stays
on the server and is never returned to the browser. No paid plan or payment handling
is implemented by TrackEase. Brevo may require transactional-email activation or
sender review before sending is permitted. When using a free-address sender, it may
replace the sender address to meet recipient-provider requirements.

## Password reset

- The login page links to `recover.jsp`, where a user enters their username.
- A generic response avoids revealing whether a username exists.
- A single-use token is stored as a SHA-256 digest and expires after 30 minutes.
- The actual token is delivered only in the email link. The reset page removes it
  from the URL fragment after reading it.
- A successful reset changes the password, invalidates prior sessions, and removes
  other pending reset tokens and saved recovery codes.
- A user can generate eight backup codes under **Security** after entering their
  current password. Only code digests are stored; the plain codes are shown once.

## Booking notifications

Booking audit events enqueue notifications in the **same database transaction** as
the reservation. A rollback removes the event and its notification together.

The dispatcher checks the outbox periodically, attempts up to ten messages per
pass, and records successful sends. Failed deliveries retry after five minutes,
up to five attempts. Only notifications created in the last day are eligible for
automatic delivery. Delivery is at-least-once: a process failure after provider
acceptance but before the database commit can produce a duplicate email.

Events include `CONFIRMED`, `WAITLISTED`, `RAC`, `CANCELLED`, `PROMOTED`, and
`PROMOTED_RAC`. Email text distinguishes the event from the current ticket status
and links to the latest authenticated ticket page.

Administrators can check whether delivery is configured at
`GET /api/admin/email-status`. This checks configuration presence, not provider
approval or inbox delivery. Provider quotas and temporary outages do not undo a
successful reservation.

## Local SMTP testing without external mail

The optional `scripts/smoke-features.cjs` creates an SMTP sink bound to
`127.0.0.1:1025`. Its messages stay in memory; no email is sent to external inboxes.
Start the application with these process-scoped settings, then run the script in
another terminal:

```powershell
$env:MAIL_PROVIDER='smtp'
$env:SMTP_HOST='127.0.0.1'
$env:SMTP_PORT='1025'
$env:SMTP_AUTH='false'
$env:SMTP_STARTTLS='false'
$env:MAIL_FROM='TrackEase <sender@example.test>'
$env:PUBLIC_BASE_URL='http://127.0.0.1:8080'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
```

```powershell
node scripts/smoke-features.cjs
```

After testing, stop the application and start it from a fresh terminal with these
test overrides cleared. Never use the local SMTP sink as your hosted email server.

## References

- [Brevo plans and free limits](https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans)
- [Brevo transactional email API](https://developers.brevo.com/docs/send-a-transactional-email)
- [Brevo sender-address requirements](https://help.brevo.com/hc/en-us/articles/14925263522578-Comply-with-Gmail-Yahoo-and-Microsoft-s-requirements-for-email-senders)
- [Render free-service restrictions](https://render.com/docs/free)
