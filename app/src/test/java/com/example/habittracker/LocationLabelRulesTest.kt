package com.example.habittracker

import com.example.habittracker.data.LocationLabelRules
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationLabelRulesTest {
    @Test fun prefersCityAndCountry() = assertEquals("Dhaka, Bangladesh", LocationLabelRules.label("Dhaka", "Dhaka District", "Dhaka Division", "Bangladesh"))
    @Test fun fallsBackThroughAdministrativeAreas() = assertEquals("Sylhet District, Bangladesh", LocationLabelRules.label(null, "Sylhet District", "Sylhet", "Bangladesh"))
    @Test fun usesSafeFallbackWhenGeocoderHasNoParts() = assertEquals("Current location", LocationLabelRules.label(null, null, null, null))
}
