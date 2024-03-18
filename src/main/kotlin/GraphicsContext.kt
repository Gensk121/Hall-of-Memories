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
        ImGui.Begin("Aggro Hall of Memories v1.02", 0)
        ImGui.SetWindowSize(350f, -1f)
        ImGui.Text("Start inside Hall of Memories")

        ImGui.BeginTabBar("Tabs", ImGuiWindowFlag.None.value)
        if (ImGui.BeginTabItem("Settings", ImGuiWindowFlag.None.value)) {

            script.keepCurrentMethod.set(
                ImGui.Checkbox(
                    "Progressive mode (95 = Incan)",
                    script.keepCurrentMethod.get()
                )
            )
            val itemSelected = ImGui.Combo("Select Memory", script.chosenMemoryIndex, *script.memories)

            ImGui.Spacing(0f, 20f)
            ImGui.Text("Targeted memory: ${script.chosenMemory}") //targeted memory label
            ImGui.Text("Penguins found: ${script.penguins}")    //penguins found label

            ImGui.EndTabItem()
        }

        if (ImGui.BeginTabItem("Stats", ImGuiWindowFlag.None.value)) {
            val elapsedTime: Long = System.currentTimeMillis() - script.startTime

            // Convert milliseconds to hours, minutes, and seconds
            val seconds = elapsedTime / 1000 % 60
            val minutes = elapsedTime / (1000 * 60) % 60
            val hours = elapsedTime / (1000 * 60 * 60) % 24

            ImGui.Separator()
            ImGui.Text(
                "Runtime: %02d:%02d:%02d%n",
                hours, minutes, seconds
            )
            ImGui.Separator()
            ImGui.Text(String.format("Xp gained %,d", script.xpGained))
            ImGui.Text(String.format("Xp/hr %,d", script.xpPerHour))
            ImGui.EndTabItem()
        }




        ImGui.End()
    }

    override fun drawOverlay() {
        super.drawOverlay()
    }
}