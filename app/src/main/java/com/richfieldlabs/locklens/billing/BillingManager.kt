package com.richfieldlabs.locklens.billing

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor() {
    val productId: String = "locklens_pro_lifetime"
}

