# PrintXpress – Android (Kotlin, native)

## Run
1. Android Studio → File → Open → select this `PrintXpress` folder.
2. Let Gradle sync (Android Studio downloads Gradle 8.7 automatically).
3. Run on an emulator/phone (Android 7.0+ / API 24+). Allow notifications when asked.

## Structure (simple – Activities + SQLite, no frameworks)
| File | Purpose |
|---|---|
| DbHelper.kt | SQLite database (SQLiteOpenHelper), tables, seed data, all queries |
| Utils.kt | BaseActivity, Session (login state), Validator, order status rules, dialogs |
| Notifier.kt | Local notifications (order updates + promotions) |
| RowAdapter.kt | One list adapter reused on every list screen |
| *Activity.kt | One file per screen |

## Features → brief
1. Auth & profile: Register/Login (email or phone), edit profile, addresses, saved designs, order history
2. Browse & order: categories, search by name/material/size, sample preview, upload artwork (PDF/image) or text, settings, pickup/delivery, date, promo code
3. Order management: status timeline, reschedule/cancel only before "Printing"
4. Notifications: order confirmed / rescheduled / cancelled / each status / completed; seasonal promo alert (can be turned off in Offers)
5. Design support: print guidelines, FAQ, call/email, "ask a design question" (saved to DB)

"Staff demo: move to next stage" on the order screen simulates the print shop updating the order.

## Validation
Email format, SL mobile (07XXXXXXXX / +947XXXXXXXX), unique email/phone, strong password (8+, letters+numbers), password match,
privacy consent, quantity 1–10,000, artwork OR text required, text ≤ 200 chars, file ≤ 25 MB, date tomorrow–60 days,
delivery needs an address, promo code must exist and meet minimum quantity, address/label length, support query length.
Passwords are stored as salted SHA-256 hashes.

## Database (3NF)
users(id, name, email UNIQUE, phone UNIQUE, password_hash)
categories(id, name UNIQUE)
products(id, category_id→categories, name, unit_price, description, color)
product_options(id, product_id→products, option_type[MATERIAL|SIZE], value)
addresses(id, user_id→users, label, address)
designs(id, user_id→users, title, file_name, file_uri, custom_text, created_at)
promotions(id, title, description, code UNIQUE, discount, min_qty)
orders(id, user_id→users, product_id→products, promo_id→promotions, material, size, quantity, custom_text,
       file_name, file_uri, delivery_method, address, schedule_date, total, status, created_at)
support_queries(id, user_id→users, subject, message, status, created_at)

View the live DB: Android Studio → View → Tool Windows → App Inspection → Database Inspector.
