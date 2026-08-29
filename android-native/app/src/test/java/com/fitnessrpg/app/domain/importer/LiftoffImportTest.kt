package com.fitnessrpg.app.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiftoffImportTest {

    private val nextData = """
        {"props":{"pageProps":{"trpcState":{"json":{"queries":[
          {"state":{"data":{"session":true}}},
          {"state":{"data":{
            "id":"abc","name":"Full Upper body ",
            "exerciseData":[
              {"exerciseIndex":0,"exerciseName":"Dumbbell Bench Press","exerciseId":"dumbbell_bench_press","superset":null,
               "setsData":[{"setIndex":0,"setType":"normal","inputOne":44.09,"inputTwo":12},
                           {"setIndex":1,"setType":"normal","inputOne":48.5,"inputTwo":10},
                           {"setIndex":2,"setType":"normal","inputOne":48.5,"inputTwo":10}]},
              {"exerciseIndex":1,"exerciseName":"Lat Pulldown","exerciseId":"lat_pulldown","superset":null,
               "setsData":[{"setIndex":0,"setType":"normal","inputOne":100,"inputTwo":10},
                           {"setIndex":1,"setType":"normal","inputOne":110,"inputTwo":8}]}
            ]}}}
        ]}}}}}
    """.trimIndent()

    @Test
    fun `parses preset name and exercises`() {
        val plan = parseLiftoffPlan(nextData)!!
        assertEquals("Full Upper body", plan.name)
        assertEquals(2, plan.exercises.size)
        val bench = plan.exercises[0]
        assertEquals("Dumbbell Bench Press", bench.name)
        assertEquals(3, bench.sets)
        assertEquals(10, bench.repsMin)
        assertEquals(12, bench.repsMax)
    }

    @Test
    fun `reps range comes from the recorded sets`() {
        val lat = parseLiftoffPlan(nextData)!!.exercises[1]
        assertEquals(2, lat.sets)
        assertEquals(8, lat.repsMin)
        assertEquals(10, lat.repsMax)
    }

    @Test
    fun `returns null on unrelated json`() {
        assertNull(parseLiftoffPlan("""{"props":{"pageProps":{}}}"""))
        assertNull(parseLiftoffPlan("not json"))
    }

    @Test
    fun `extractNextData pulls the script payload`() {
        val html = """<html><body><script id="__NEXT_DATA__" type="application/json">{"a":1}</script></body></html>"""
        assertEquals("""{"a":1}""", extractNextData(html))
    }

    // ---- matching ----

    private val catalog = listOf(
        MatchCandidate("1", "Dumbbell Bench Press"),
        MatchCandidate("2", "Cable Lat Pulldown"),
        MatchCandidate("3", "Barbell Squat"),
        MatchCandidate("4", "Tricep Rope Pushdown"),
    )

    @Test
    fun `exact name matches`() {
        assertEquals("1", matchExercise("Dumbbell Bench Press", catalog))
    }

    @Test
    fun `subset name matches the containing catalog entry`() {
        // "Lat Pulldown" ⊂ "Cable Lat Pulldown"
        assertEquals("2", matchExercise("Lat Pulldown", catalog))
    }

    @Test
    fun `unrelated name does not match`() {
        assertNull(matchExercise("Treadmill", catalog))
    }

    @Test
    fun `near match on shared tokens matches`() {
        assertEquals("4", matchExercise("Rope Pushdown", catalog))
    }

    @Test
    fun `end to end parse then match`() {
        val plan = parseLiftoffPlan(nextData)!!
        val matched = plan.exercises.map { matchExercise(it.name, catalog) }
        assertEquals("1", matched[0])
        assertEquals("2", matched[1])
        assertTrue(matched.all { it != null })
    }
}
