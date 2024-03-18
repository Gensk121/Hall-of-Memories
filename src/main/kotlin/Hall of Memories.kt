package com.aggro

import net.botwithus.api.game.hud.inventories.Backpack
import net.botwithus.internal.scripts.ScriptDefinition
import net.botwithus.rs3.events.impl.SkillUpdateEvent
import net.botwithus.rs3.game.Client
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc
import net.botwithus.rs3.script.Execution
import net.botwithus.rs3.script.LoopingScript
import net.botwithus.rs3.script.config.ScriptConfig
import java.util.Random
import net.botwithus.rs3.game.movement.Movement
import net.botwithus.rs3.game.skills.Skills
import net.botwithus.rs3.imgui.NativeBoolean
import net.botwithus.rs3.imgui.NativeInteger

class HallofMemories(
    name: String,
    scriptConfig: ScriptConfig,
    scriptDefinition: ScriptDefinition
) : LoopingScript (name, scriptConfig, scriptDefinition) {

    var xpGained: Int = 0
    var xpPerHour = 0
    var startTime = System.currentTimeMillis()
    private val DEPOT_ID = 111374 //Object ID of the depot
    private val RIFT_ID = 111375 //Object ID of the rift
    private var checkForAgentsEnabled = true //while true, spies on Penguins
    private var selectedMemory: Npc? = null
    var memories = arrayOf("Lustrous memories", "Brilliant memories", "Radiant memories", "Luminous memories", "Incandescent memories")
    var chosenMemoryIndex: NativeInteger = NativeInteger(0)
    var chosenMemory = "" //this is the memory that is currently being used
    var keepCurrentMethod: NativeBoolean = NativeBoolean(true)
    var penguins = 0
    private var attempts = 0
    private val random: Random = Random()

    override fun initialize(): Boolean {
        loopDelay = 500
        console.addLineToConsole("Aggro Hall of Memories loaded")
        sgc = GraphicsContext(this, console)
        keepCurrentMethod.set(true)

        subscribe(SkillUpdateEvent::class.java) {
            val xpChange = it.experience - it.oldExperience
            if (xpChange in -10_000_000..10_000_000) { // Check if the XP change is between -10 million and 10 million
                xpGained += xpChange
            }
        }

        return super.initialize()
    }

    override fun onLoop() {
        try {
            val levels = Skills.DIVINATION.level
            console.addLineToConsole(levels.toString())
            updateStatistics()
            chosenMemory = middleNPCs()  //checks to see if memory bud is open, in the middle
                ?: if (!keepCurrentMethod.get()) {
                    memories[chosenMemoryIndex.get()]
                } else {
                val divinationLevel = Skills.DIVINATION.level// Gets current divination level
                    console.addLineToConsole(divinationLevel.toString())
                    when {
                    divinationLevel in 70..79 -> "Lustrous memories" // If Div Level 70-79, chosen memory = "Lustrous memories"
                    divinationLevel in 80..84 -> "Brilliant memories"
                    divinationLevel in 85..89 -> "Radiant memories"
                    divinationLevel in 90..94 -> "Luminous memories"
                    divinationLevel >= 95 -> "Incandescent memories"
                    else -> ""
                    }
            }
            console.addLineToConsole(chosenMemory)
            val jarSearch = Backpack.getItems()
            val fullJarCheck =  jarSearch.any { item ->
                item.name?.contains("Memory jar" ) == true && !item.name?.equals("Memory jar (full)")!!
            }
            when {
                    checkForAgents() != null -> spyOnAgent() //checks to see if agent is present (see function)
                    checkKnowledgeFragment() != null -> captureKnowledgeFragment()  //checks to see if knowledge fragment is present (see function)
                    jarSearch.any { item ->
                        item.name?.contains("Memory jar") ?: false } && fullJarCheck -> {  //checks to see if backpack is not full and if there is a memory jar that is not full (see function)
                        fillJars(); startTwoTicking()   //fills jars (see function)
                    }
                    jarSearch.any { item ->
                        item.name?.equals("Memory jar (full)") == true && !item.name?.equals("Memory Jar (empty)")!!
                    } -> depositJars()
                    jarSearch.any { item ->
                        item.name?.equals("Memory jar (empty)") == false } || Backpack.isEmpty() -> grabJars()
                    else -> { Execution.delay(random.nextInt(500, 1525).toLong())
                    console.addLineToConsole("elsed") }
                }
        } catch (e: Exception) {
            console.addLineToConsole(e.message)
        }

        return
    }

    private fun grabJars(): Int { // grab jars from depot when inventory has space
        val player = Client.getLocalPlayer()
        val depot = SceneObjectQuery.newQuery() //How to interact with game objects
            .id(DEPOT_ID)
            .option("Take-from") //option used when interacting with object ingame
            .results()
            .firstOrNull() // can't find object, returns null
        if (checkForAgents() != null) {
            spyOnAgent() //checks to see if agent is present (see function)
        }
        if (player != null) {
            if (depot != null && Client.getLocalPlayer()?.isMoving == false) {
                console.addLineToConsole("grabbing jars")
                depot.interact("Take-from") //take jars from depot
                Execution.delayUntil(3000) {
                    Client.getLocalPlayer()?.isMoving == false
                }
                var freeSpace = Backpack.countFreeSlots()
                var i = 0
                while (freeSpace > 2 && i <=300) {
                    Execution.delay(150)
                    freeSpace = Backpack.countFreeSlots()
                    i++
                }
                val coord = Client.getLocalPlayer()?.coordinate
                if (coord != null) {
                    console.addLineToConsole("running to middle")
                    Movement.walkTo(coord.x.minus(18), coord.y, false)
                }
                Execution.delay(random.nextInt(1874, 5301).toLong())
            }
        }

        return 0
    }

    private fun findSelectedMemory(): Npc? { //finds the memory that is selected in the combo box or progressive mode
        return NpcQuery.newQuery()
            .name(chosenMemory)
            .results()
            .nearest()
    }

    private fun fillJars(): Int {   //fills jars with memories
       val player = Client.getLocalPlayer()
       selectedMemory = findSelectedMemory()
        if(player != null) {
            Execution.delayUntil(5000) {
                Client.getLocalPlayer()?.isMoving == false
            }
            console.addLineToConsole("Fill Jars")
            if (Client.getLocalPlayer()?.isMoving == false) {
                    Execution.delay(random.nextInt(500, 1225).toLong())
                    selectedMemory?.interact("Harvest")
                }
            }

        return 0
    }

    private fun startTwoTicking() { //Two-ticks chosen memories
        val memory = findSelectedMemory()
        var player = Client.getLocalPlayer()?.animationId

            while (player != -1) {
                    catchNPCs()
                    if(checkForAgents() != null){
                        spyOnAgent()
                    } else if (checkKnowledgeFragment() != null){
                        captureKnowledgeFragment()
                    } else {
                        console.addLineToConsole("Two-tick")
                        memory?.interact("Harvest")
                        Execution.delay(random.nextInt(1200, 1250).toLong())
                    }
                    player = Client.getLocalPlayer()?.animationId
            }
    }


    private fun depositJars() { //deposits jars into the rift
        val rift = SceneObjectQuery.newQuery()
            .id(RIFT_ID)
            .results()
            .firstOrNull()
        if (checkForAgents() != null) {
            spyOnAgent()
        }
        if (rift != null) {
            rift.interact("Offer-memory")
            console.addLineToConsole("Depositing Jars")
            Execution.delayUntil(60000) {
                !Backpack.getItems().any { item ->
                    item.name?.contains("jar", ignoreCase = true) ?: false
                }
            }
            console.addLineToConsole("Done with Jars")

        }
    }

    private fun checkForCoreFragment(): Npc? { //unused atm, but will be used to check for core fragments
        return NpcQuery.newQuery()
            .name("Core memory fragment")
            .results()
            .nearest()
    }

    private fun captureCoreFragment() { //unused atm, but will be used to capture core fragments
        val coreFrag = checkForCoreFragment()
        if (coreFrag != null && !Backpack.isFull()) {
            do {
                coreFrag.interact("Capture")
                Execution.delay(random.nextInt(250, 627).toLong())
                Execution.delayUntil(3000) {
                    Client.getLocalPlayer()?.isMoving == false
                }
            } while (checkForCoreFragment() != null  && isActive)
        }
    }

    private fun checkKnowledgeFragment(): Npc? { //checks for knowledge fragments
        return NpcQuery.newQuery()
            .name("Knowledge fragment")
            .results()
            .firstOrNull()
    }

    private fun captureKnowledgeFragment() {  //captures knowledge fragments
        if (checkKnowledgeFragment() != null) {
            val coreFrag = NpcQuery.newQuery()
                .name("Knowledge fragment")
                .results()
                .nearest()

            coreFrag?.interact("Capture")
            Execution.delayUntil(5000) {
                Client.getLocalPlayer()?.isMoving == false
            }

            Execution.delay(random.nextInt(250, 627).toLong())
        }
    }

    private fun checkForAgents(): String? { //checks for secret agents, disabled after weekly limit hit
        if(!checkForAgentsEnabled) return null
            val agent = NpcQuery.newQuery()
                .name("Agent", String::contains)
                .results()
                .firstOrNull()
            if (agent != null) {
                agent.interact("Spy-on")
                Execution.delay(random.nextInt(2250, 4627).toLong())

                val newAgent = NpcQuery.newQuery()
                    .name("Agent", String::contains)
                    .results()
                    .firstOrNull()
                    attempts++
                if(newAgent == null){
                    attempts = 0
                    return agent.name
                }
            }
            if (attempts >= 3) {
                checkForAgentsEnabled = false
                console.addLineToConsole("Weekly limit hit, disabling agent checks")
                return null
            }
        return null
    }

    private fun spyOnAgent() { //returns no. of agents found
            if (!checkForAgentsEnabled && checkForAgents() != null) return
            else
            penguins++
            console.addLineToConsole("Number of penguins found: $penguins")
    }

     private fun middleNPCs(): String? { //Used to prioritise special memories found in the middle
        val npcNames = listOf("Juna", "Aagi", "Seren", "Sword of Edicts", "Cres")
        for (name in npcNames) {
            val npc = NpcQuery.newQuery()
                .name(name)
                .results()
                .firstOrNull()
            if (npc != null) {
                return npc.name
            }
        }
        return null
    }

    private fun findCatchables(): String? {
        val npcNames = arrayOf("Seren spirit", "Divine blessing", "Manifested knowledge", "Catalyst of alteration" )
        npcNames .forEach {
            if (findNpc(it) != null) return it
        }
        return null
    }

    private fun catchNPCs() {
        val npcToCatch = findCatchables() ?: return
        if (Backpack.isFull()) {
            return
        }

        val popupNpc = NpcQuery.newQuery()
            .name(npcToCatch)
            .results()
            .nearest()

        if (popupNpc != null) {
            when (popupNpc.name) {
                "Manifested knowledge" -> {
                    popupNpc.interact("Siphon")
                    Execution.delay(random.nextInt(250, 427).toLong())
                }
                else -> {
                    popupNpc.interact("Capture")
                    Execution.delay(random.nextInt(225, 427).toLong())
                }
            }
        }
    }

    private fun findNpc(npcName: String): Npc? {
        return NpcQuery.newQuery()
            .name(npcName)
            .results()
            .nearest()
    }

    private fun updateStatistics() {
        val currentTime: Long = (System.currentTimeMillis() - startTime) / 1000

        xpPerHour = Math.round(3600.0 / currentTime * xpGained).toInt()
    }
}