package com.gamilo.app.data.model

enum class JobStatus { ACTIVE, ON_HOLD, COMPLETED, CANCELLED }

enum class TaskPriority { LOW, NORMAL, HIGH, URGENT }

enum class ShippingCarrier { CANADA_POST, FEDEX, UPS, DHL, OTHER }

/** Who absorbs a shipment's freight/insurance cost — the business, or the client it's billed to. */
enum class CoverageParty { SELLER, CLIENT }

enum class AttachmentOwnerType { JOB, EXPENSE, SHIPPING }

/**
 * Which currency new entries default to. Gamilo's tax jurisdiction (BC GST+PST) is fixed
 * regardless of this setting — Region only seeds baseCurrencyCode/manualFxRateToCad
 * defaults for new rows; it never touches currencyCode/fxRateApplied already frozen on
 * past records.
 */
enum class Region { CANADA, UNITED_STATES }
