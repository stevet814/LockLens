# LockLens — Play Store Listing

## Title (30 chars max)
`LockLens – Private Photo Vault`

## Short Description (80 chars max)
`Private camera + encrypted vault. No metadata. No cloud. No account.`

## Full Description (4000 chars max)

LockLens is the only app that does both: strips your photo's location data the moment it's taken, then locks everything in an AES-256 encrypted vault only you can open.

**The problem with your camera app**
Every photo you take with Android embeds your GPS location, device model, and exact timestamp. Share that photo and you've shared your home address. Every photo vault app in the top results has been flagged for spyware or aggressive data collection. LockLens was built to fix both problems at once.

**How it works**
Shoot with the LockLens camera and EXIF metadata is stripped before the file ever hits storage — no location, no device fingerprint, no timestamp. Then the photo is AES-256-GCM encrypted and stored in your device's private app storage, where only LockLens can read it. Unlock with fingerprint, face, or a vault-only PIN.

**Free tier**
- Secure camera (all photos are EXIF-clean)
- Encrypted vault (up to 100 photos)
- Biometric unlock
- Import photos from your gallery (they're stripped and encrypted too)

**Pro — $4.99 once, no subscription**
- Unlimited photos and videos
- Video vault with in-app playback
- Decoy PIN — a second PIN that opens a fake empty vault instead of your real one
- Break-in selfie — failed unlock attempts secretly snap a front-camera photo of whoever's trying to get in. It's encrypted and stored in your intrusion log.
- Secure share — share a photo from the vault without it ever being saved to your gallery
- Photo albums

**Why no subscription?**
Other vault apps charge $3.99/month — that's $48 a year, forever, just to store your own files on your own phone. LockLens charges $4.99 once. That's less than one month of the competition, and you own it.

**Technical details for the skeptics**
- AES-256-GCM encryption with per-file unique IVs
- Keys stored in Android Keystore (hardware-backed on supported devices)
- No INTERNET permission — confirmed in the manifest, verified in the Play Console Data Safety section
- No analytics, no crash reporting, no ad network SDKs
- `android:allowBackup="false"` — your encrypted files will never appear in a cloud backup
- Network security config blocks all outbound connections, even if a future update accidentally added an INTERNET permission
- PIN stored as PBKDF2-SHA256 (200,000 iterations) — not plaintext, not a simple hash
- Open to security researchers: if you find a vulnerability, email steve@richfieldlabs.com

**Permissions**
- `CAMERA` — for the secure camera and break-in selfies
- `USE_BIOMETRIC` — for fingerprint/face unlock
- `com.android.vending.BILLING` — for the one-time $4.99 Pro purchase
- `POST_NOTIFICATIONS` — to notify you when a break-in selfie is captured

That's it. No contacts, no storage, no location, no internet.

---

## Category
Tools → Privacy

## Tags (Play Console)
- Photo vault
- Private photos
- Encrypted photos
- Privacy
- Secure camera

## Content Rating
Everyone (PEGI 3)

## Data Safety (Play Console questionnaire)

**Does your app collect or share any required user data types?**
No. All data stays on device.

**Does your app collect photos or videos?**
Yes — stored locally only, encrypted, never shared.

**Is all data encrypted in transit?**
N/A — no data is ever transmitted.

**Can users request deletion?**
Yes — delete individual photos or uninstall to remove everything.

---

## Pricing
- Free tier: available at no cost
- Pro: $4.99 (one-time in-app purchase, SKU: `locklens_pro_lifetime`)

---

## Privacy Policy URL
`https://richfieldlabs.github.io/locklens-privacy`

## App Website
`https://richfieldlabs.github.io/locklens`

---

## Screenshot captions (8 screenshots)

1. **Lock screen** — "Your vault. Locked tight."
2. **Vault grid** — "AES-256 encrypted. Biometric unlock."
3. **Camera viewfinder** — "No GPS. No fingerprint. No timestamp."
4. **Photo detail** — "Pinch to zoom. Share securely."
5. **Settings** — "Auto-lock. Configurable timeouts."
6. **Decoy PIN** — "Second PIN opens a fake vault."
7. **Intrusion log** — "See who tried. Encrypted selfie included."
8. **Pro upgrade** — "One payment. No subscription. Ever."
