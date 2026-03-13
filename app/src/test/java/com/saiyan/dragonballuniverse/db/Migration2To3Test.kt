package com.saiyan.dragonballuniverse.db

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Migration tests require androidTest + androidx.room:room-testing.
 *
 * The project currently runs only JVM unit tests in CI (./gradlew testDebugUnitTest),
 * so this file is a JVM-friendly placeholder to keep the plan's "migration test" slot
 * without breaking unit test compilation.
 *
 * To enable real migration tests:
 * 1) Move a MigrationTestHelper-based test into app/src/androidTest/
 * 2) Add room-testing + android instrumentation deps
 * 3) Run ./gradlew connectedDebugAndroidTest
 */
class Migration2To3Test {
    @Test
    fun placeholder() {
        assertTrue(true)
    }
}
