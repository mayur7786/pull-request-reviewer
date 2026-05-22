# AI Code Review

## Intent

Initial commit for subscription calculator Branch suggests billing sbscription calc. Touches price model, subscription calculator.

Confidence: 0.85

## Summary

Core subscription calculation logic is present, but there are several correctness and robustness gaps around null handling, tier ordering, and discount validation that can lead to runtime exceptions or incorrect billing results. No tests are shown for key branches.

Reviewed 2 Java files.

## High Severity

- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:23 [BUG] calculate(...) can throw NullPointerException if request or request.lineItems() is null.
  - Evidence: Method signature accepts BillingRequestDto request without null checks, then iterates: `for (LineItemDto item : request.lineItems()) {`
  - Suggestion: Validate inputs at the start of calculate(...), e.g., requireNonNull(request) and treat null lineItems as empty or throw a clear IllegalArgumentException.
  - Confidence: 0.78
- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:49 [BUG] resolveUnitPrice(...) can throw NullPointerException when priceModel is null (or tiers is null for tiered pricing).
  - Evidence: Uses `if (item.priceModel() == PriceModel.FLAT)` (safe for null comparison), but else branch unconditionally calls `return item.tiers().stream()` which will NPE if `item.tiers()` is null. Also if `item.priceModel()` is null, code falls into tier logic unexpectedly.
  - Suggestion: Explicitly validate `item.priceModel()` and, for tiered pricing, validate `item.tiers()` is non-null/non-empty before streaming; otherwise throw a clear IllegalArgumentException.
  - Confidence: 0.80
- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:56 [BUG] Tier selection depends on stream encounter order; if multiple tiers match, the first one is chosen, which can yield incorrect pricing if tiers are unsorted or overlapping.
  - Evidence: Tier resolution: `.filter(tier -> tier.matches(item.quantity())).findFirst()` selects the first matching tier in list order.
  - Suggestion: Define and enforce tier ordering/uniqueness (e.g., sort tiers by min quantity descending/ascending as required, or validate no overlaps) before selecting a tier.
  - Confidence: 0.72

## Medium Severity

- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:41 [BUG] money(...) will throw NullPointerException if passed a null BigDecimal (e.g., if any upstream dto field like unitPrice is null).
  - Evidence: `private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }` has no null guard; upstream uses `item.unitPrice()` and `TierDto::unitPrice` without null checks.
  - Suggestion: Validate required monetary inputs (unitPrice, tier unitPrice, discount value) are non-null before arithmetic; fail fast with a clear exception.
  - Confidence: 0.68
- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:64 [BUG] Percentage discount calculation does not validate discount.value() range; values > 100 or negative can produce negative amounts or amounts exceeding subtotal.
  - Evidence: For percentage: `subtotal.multiply(discount.value()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);` with no checks on `discount.value()`.
  - Suggestion: Validate percentage discount value is within [0, 100] (or your business rules) and reject/normalize invalid values to prevent negative or inflated totals.
  - Confidence: 0.70
- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:70 [BUG] For non-percentage discounts, the code returns discount.value() directly, which can exceed subtotal or be negative; later logic clamps displayed discount to subtotal but uses unclamped discount for amount calculation.
  - Evidence: Non-percentage path: `return discount.value();` then in calculateLineItem: `BigDecimal amount = subtotal.subtract(discountAmount); if (amount.signum() < 0) { amount = BigDecimal.ZERO; }` and response uses `money(discountAmount.min(subtotal))` (clamped) while amount used unclamped discount.
  - Suggestion: Clamp/validate fixed discount at calculation time (e.g., `min(subtotal)` and non-negative) so discountAmount and amount remain consistent and predictable.
  - Confidence: 0.74

## Low Severity

- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:20 [TESTING] No tests are shown for key calculation branches and edge cases (tier selection, percentage rounding, discount clamping, missing tier).
  - Evidence: Only production code is provided; no test files or test coverage is included in the context for calculateLineItem/resolveUnitPrice/calculateDiscount.
  - Suggestion: Add unit tests covering: FLAT vs TIER pricing, tier not found exception, overlapping tiers behavior, percentage discount rounding, fixed discount > subtotal, negative/over-100 percentage validation, and null/empty inputs if supported.
  - Confidence: 0.60
- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:72 [MAINTAINABILITY] TODO indicates incomplete support for FIXED discount type; current behavior applies discount.value() for any non-percentage type, which may be incorrect if additional types exist.
  - Evidence: Comment: `//TODO add logic for FIXED discount type when supported` and fallback `return discount.value();` for all non-percentage types.
  - Suggestion: Handle discount types explicitly (e.g., switch on DiscountType) and throw for unsupported types to avoid silently incorrect calculations.
  - Confidence: 0.66

## Testing Gaps

- src/main/java/com/example/subscription/service/SubscriptionCalculator.java:20 No tests are shown for key calculation branches and edge cases (tier selection, percentage rounding, discount clamping, missing tier).
