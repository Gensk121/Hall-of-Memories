package com.aggro

import net.botwithus.rs3.imgui.ImGui
import net.botwithus.rs3.imgui.ImGuiWindowFlag
import net.botwithus.rs3.script.ScriptConsole
import net.botwithus.rs3.script.ScriptGraphicsContext

class GraphicsContext(
    private val script: HallofMemories,
    console: ScriptConsole
) : ScriptGraphicsContext (console) {

    override fun drawSettings() {
        super.drawSettings()
        ImGui.Begin("Aggro Hall of Memories v1.0", 0)
        ImGui.SetWindowSize(350f, -1f)
        ImGui.Text("Start inside Hall of Memories")

        script.keepCurrentMethod.set(ImGui.Checkbox("Progressive mode (95 = Incan)", script.keepCurrentMethod.get()))
        val itemSelected = ImGui.Combo("Select Memory", script.chosenMemoryIndex, *script.memories)

        ImGui.Spacing(0f, 20f)
        ImGui.Text("Targeted memory: ${script.chosenMemory}") //targeted memory label
        ImGui.Text("Penguins found: ${script.penguins}")    //penguins found label

        ImGui.End()
    }

    override fun drawOverlay() {
        super.drawOverlay()
    }
}