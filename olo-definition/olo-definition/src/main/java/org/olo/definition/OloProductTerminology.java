package org.olo.definition;

/**
 * Canonical product naming for the Open LLM Orchestrator platform.
 * Use {@link #PRODUCT} in user-facing text, prompts, and documentation.
 */
public final class OloProductTerminology {

    public static final String FULL_NAME = "Open LLM Orchestrator";
    public static final String ACRONYM = "OLO";
    /** Standard reference: full name followed by acronym. */
    public static final String PRODUCT = FULL_NAME + " (" + ACRONYM + ")";
    /** Workflow graph owned by the platform. */
    public static final String WORKFLOW = PRODUCT + " workflow";

    private OloProductTerminology() {
    }

    /** e.g. {@code "You are a research planning agent for the Open LLM Orchestrator (OLO)."} */
    public static String agentRolePrompt(String roleDescription) {
        return "You are a " + roleDescription + " agent for the " + PRODUCT + ".";
    }
}
