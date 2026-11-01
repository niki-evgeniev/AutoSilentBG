# Promotions

Customers browse `/promocii` and `/promocii/{product-slug}`. Numeric legacy URLs
redirect permanently to the readable URL. The first offer uses the base product
slug; additional offers get a pack-size suffix and a counter if necessary.
Slugs are stored and remain stable when the price or pack size changes. Existing
offers receive slugs automatically at startup. Product cards are shared with
the standard catalog through `fragments/catalog-card.html`. Search, stock filter,
price ordering and pagination are available. Navigation links to Promotions.

Administrators manage offers at `/admin/settings/promotions`: base product,
units per pack, total pack price, active flag, optional start/end and hide-from-
catalog flag. Unchecking Active stops an offer without deleting its history.
Empty start/end values mean no respective time limit. Times use the application
server's local time, consistent with checkout. Invalid date ranges are rejected.

Hiding affects the `/shumoizolaciya` catalog and its category/filter results only
while a matching offer is active and in its date window. It does not deactivate
the base product. If several offers hide a product, it returns when none of those
offers remains live. Direct base-product links remain available.

One cart quantity represents one pack. Promotion lines have a distinct cart
identity and submit both the base product ID and promotion ID. Checkout reloads
the current offer server-side, validates product identity and validity dates,
uses the saved price and deducts pack count × pack size from shared inventory.
Kit offers multiply their component allocations too. Insufficient stock rolls
back the whole order. Closed offers are rejected at checkout.

Promotion prices never receive customer or promo-code discounts. Mixed orders
apply discounts only to ordinary lines; promotion-only orders ignore codes.
The same rule applies to subsequent order-item recalculation and manual admin
price adjustments. Manual order totals cannot undercut the promotional portion.

Order lines snapshot the promotion ID, unit price, pack size, label and inventory
allocation. Later offer changes do not change existing purchases. Removing an
order line returns its original allocation. Delivered-sales statistics count the
actual base-product units, rather than just the number of packs.

Hibernate's configured `ddl-auto: update` creates `promotions` and the new nullable
order-item promotion ID plus `units_per_item` (default 1 for existing order lines).
There are no new external services or credentials.
