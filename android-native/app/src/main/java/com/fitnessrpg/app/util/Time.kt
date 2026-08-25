package com.fitnessrpg.app.util

import java.time.Instant

/** Epoch millis -> ISO-8601 UTC string (e.g. "1970-01-01T00:00:00Z"). */
fun isoFromMillis(ms: Long): String = Instant.ofEpochMilli(ms).toString()

/** Parse an ISO-8601 instant back to epoch millis. */
fun millisFromIso(iso: String): Long = Instant.parse(iso).toEpochMilli()
