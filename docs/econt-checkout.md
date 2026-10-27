# Product questions and Econt delivery

Product details include a collapsible question form. Signed-in customers can edit
their prefilled contact details. Questions appear in `/admin/inquiries` with a
`[QUESTION]` subject and the product name, URL, SKU and phone in the message.
Questions require a name, valid phone, email and at least five characters of text.
The server permits one product question per IP address per minute; CSRF protection
also applies. The Viber link uses the site's existing number.

Cart checkout now has two steps: `/orders/checkout` for customer details and
`/orders/checkout/delivery` for Econt office or address delivery. Draft customer
and delivery details stay in browser session storage for up to 24 hours and are
bound to the current signed-in email (or guest session). No order or stock
reservation is created until final confirmation. Prices, discounts, stock and the
chosen city/office are checked on the server. Payment remains cash on delivery.
Courier charges are paid separately by the recipient and are excluded from the
product total. No waybill is generated automatically.

## Directory updates

`econt_cities`, `econt_offices` and `econt_sync_state` store the directory locally.
The current Hibernate `ddl-auto: update` setting creates these tables and expands
the existing order-address city/address columns when the application starts.
An initial download runs approximately 15 seconds after startup if no successful
sync exists; updates then run daily at 03:00 Europe/Sofia.

Administrators can inspect the last successful update and retry manually at
`/admin/settings/econt`, linked from the order list. No Econt credentials are
required for these public nomenclature requests. The server needs outbound HTTPS
access to `ee.econt.com`.

Both lists are fetched and validated before being saved in a single transaction.
Failed updates keep previous records and record an error. Missing entries become
inactive. Lockers and mobile stations are excluded. Submitted orders retain a
snapshot of the city, postcode, office name/code and address; later directory
changes do not affect existing orders. Old orders and the quick-order flow retain
their existing delivery behavior.

Optional application properties:

```yaml
AutoSilent:
  econt:
    sync-enabled: true
    sync-cron: "0 0 3 * * *"
```

Tests disable scheduled downloads and use deterministic directory fixtures.
The existing free-delivery banner is preserved for the planned future feature.
