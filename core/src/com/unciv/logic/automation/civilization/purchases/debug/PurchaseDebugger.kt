package com.unciv.logic.automation.civilization.purchases.debug

import com.unciv.logic.automation.civilization.purchases.core.PurchaseOption
import com.unciv.models.ruleset.nation.Personality

object PurchaseDebugger {
    private var isEnabled = true
    private val stringBuilder = StringBuilder()
    
    /**
     * Initializes a new debug session with the civilization's current state.
     */
    fun startNewSession(civName: String, gold: Int, goldPerTurn: Float) {
        stringBuilder.clear()
        appendHeader(civName, gold, goldPerTurn)
    }
    
    /**
     * Logs the evaluation of purchase options for a specific strategy.
     */
    fun addStrategyEvaluation(
        strategyName: String,
        options: List<PurchaseOption>
    ) {
        if (!isEnabled) return
        
        appendSection("Strategy: $strategyName")
        if (options.isEmpty()) {
            append("  No purchase options available")
            return
        }

        // Group options by type and show best/worst
        options.groupBy { it.type }.forEach { (type, typeOptions) ->
            val best = typeOptions.maxByOrNull { it.baseValue / it.cost }
            val worst = typeOptions.minByOrNull { it.baseValue / it.cost }
            
            appendSubsection("$type Options (${typeOptions.size} total)")
            best?.let { appendOption("Best", it) }
            worst?.let { appendOption("Worst", it) }
        }
    }
    
    /**
     * Logs the final purchase decision made by the AI.
     */
    fun addFinalDecision(
        selectedOption: PurchaseOption?,
        finalScore: Float?,
        reason: String
    ) {
        if (!isEnabled) return
        
        appendSection("Final Decision")
        if (selectedOption == null) {
            append("No purchase selected: $reason")
            return
        }
        
        appendOption("Selected", selectedOption)
        append("Final Score: $finalScore")
        append("Reason: $reason")
    }
    
    /**
     * Formats and appends the header for the debug log.
     */
    private fun appendHeader(civName: String, gold: Int, goldPerTurn: Float) {
        append("=".repeat(50))
        append("Purchase Evaluation for $civName")
        append("Available Gold: $gold")
        append("Gold per Turn: $goldPerTurn")
        append("=".repeat(50))
    }
    
    /**
     * Formats and appends a new section title.
     */
    private fun appendSection(title: String) {
        append("\n" + "=".repeat(20))
        append(title)
        append("=".repeat(20))
    }
    
    /**
     * Formats and appends a new subsection title.
     */
    private fun appendSubsection(title: String) {
        append("\n" + "-".repeat(10))
        append(title)
        append("-".repeat(10))
    }
    
    /**
     * Formats and appends a purchase option under a given label (e.g., Best, Worst, Selected).
     */
    private fun appendOption(label: String, option: PurchaseOption) {
        append("$label: ${option.description}")
        append("  Value: ${option.baseValue}")
        append("  Cost: ${option.cost}")
        append("  Value/Cost: ${String.format("%.2f", option.baseValue / option.cost)}")
    }
    
    /**
     * Appends a line of text to the debug log.
     */
    private fun append(text: String) {
        stringBuilder.append(text).append("\n")
    }
    
    /**
     * Retrieves the complete debug log as a string.
     */
    fun getDebugLog(): String = stringBuilder.toString()
    
    /**
     * Enables or disables the debugger.
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Inner object responsible for formatting the debug information.
     */
    object PurchaseDebugFormatter {
        data class PurchaseDebugInfo(
            val civName: String,
            val gold: Int,
            val goldPerTurn: Float,
            val allPurchaseOptions: List<PurchaseOption>,
            val optionsByType: Map<PurchaseOption.PurchaseType, List<PurchaseOption>>,
            val selectedPurchase: PurchaseOption?,
            val finalScore: Float?,
            val reasons: List<String>
        )

        /**
         * Formats the debug information into a readable string.
         */
        fun formatDebugOutput(debugInfo: PurchaseDebugInfo): String {
            val sb = StringBuilder()
            
            // Header
            sb.appendLine("=".repeat(50))
            sb.appendLine("Purchase Analysis for ${debugInfo.civName}")
            sb.appendLine("Available Gold: ${debugInfo.gold}")
            sb.appendLine("Gold per Turn: ${debugInfo.goldPerTurn}")
            sb.appendLine("=".repeat(50))
            
            // All Purchase Options
            sb.appendLine("\nAll Possible Purchase Options:")
            if (debugInfo.allPurchaseOptions.isEmpty()) {
                sb.appendLine("  No available purchase options.")
            } else {
                debugInfo.allPurchaseOptions.forEach { option ->
                    sb.appendLine("  [${option.type}] ${option.description}")
                    sb.appendLine("    Cost: ${option.cost}")
                    sb.appendLine("    Base Value: ${option.baseValue}")
                    sb.appendLine("    Value/Cost Ratio: ${String.format("%.2f", option.baseValue / option.cost)}")
                }
            }

            // Options Grouped by Type with Best and Worst
            debugInfo.optionsByType.forEach { (type, options) ->
                sb.appendLine("\n$type Options:")
                if (options.isEmpty()) {
                    sb.appendLine("  No options available for this type.")
                    return@forEach
                }

                val best = options.maxByOrNull { it.baseValue / it.cost }
                val worst = options.minByOrNull { it.baseValue / it.cost }

                best?.let {
                    sb.appendLine("  Best: ${it.description}")
                    sb.appendLine("    Value/Cost Ratio: ${String.format("%.2f", it.baseValue / it.cost)}")
                }
                worst?.let {
                    sb.appendLine("  Worst: ${it.description}")
                    sb.appendLine("    Value/Cost Ratio: ${String.format("%.2f", it.baseValue / it.cost)}")
                }
            }

            // Final Decision
            sb.appendLine("\nFinal Decision:")
            if (debugInfo.selectedPurchase != null) {
                sb.appendLine("  Selected Purchase: ${debugInfo.selectedPurchase.description}")
                sb.appendLine("    Type: ${debugInfo.selectedPurchase.type}")
                sb.appendLine("    Cost: ${debugInfo.selectedPurchase.cost}")
                sb.appendLine("    Base Value: ${debugInfo.selectedPurchase.baseValue}")
                sb.appendLine("    Final Score: ${String.format("%.2f", debugInfo.finalScore ?: 0f)}")
            } else {
                sb.appendLine("  No purchase selected.")
            }

            // Reasons for Selection or Rejection
            if (debugInfo.reasons.isNotEmpty()) {
                sb.appendLine("\nReasons:")
                debugInfo.reasons.forEach { reason ->
                    sb.appendLine("  - $reason")
                }
            }

            return sb.toString()
        }
    }
}