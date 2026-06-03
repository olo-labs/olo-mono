package io.olo.definition.node;

/**
 * Well-known node types for AI orchestration graphs.
 * {@link NodeDefinition#getType()} is stored as a string so custom types remain valid.
 */
public enum NodeType {

    INPUT,
    OUTPUT,
    MODEL,
    VECTOR_SEARCH,
    TOOL,
    CONDITION,
    ROUTER,
    PARALLEL,
    SUBWORKFLOW,
    HUMAN,
    MEMORY,
    RETRIEVER,
    AGENT;

    public String value() {
        return name();
    }
}
