package at.specure.measurement.coverage.domain.models

/**
 * Raised when the coverage measurement is restricted to a single IP protocol (expert-mode IPv4-only
 * or IPv6-only) and that protocol's public IP has become unavailable while the other protocol is
 * still reachable. In that situation the measurement must not silently continue on the other protocol
 * (which would record fences without ping = grey points), so it is terminated and the UI shows a
 * "restricted protocol no longer available" alert before the results overview.
 */
class RestrictedProtocolUnavailableException :
    Exception("The IP protocol the coverage measurement is restricted to is no longer available")
