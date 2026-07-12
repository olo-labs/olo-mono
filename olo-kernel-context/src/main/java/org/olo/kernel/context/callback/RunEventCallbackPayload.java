/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context.callback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Request body for POST /api/runs/{runId}/events (olo backend AppendEventRequest shape).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RunEventCallbackPayload {

    private final Long sequenceNumber;
    private final Integer eventVersion;
    private final String eventType;
    private final String correlationId;
    private final String nodeId;
    private final String parentNodeId;
    private final String nodeType;
    private final String status;
    private final Map<String, Object> output;
    private final Map<String, Object> metadata;

    public RunEventCallbackPayload(
            @JsonProperty("sequenceNumber") Long sequenceNumber,
            @JsonProperty("eventVersion") Integer eventVersion,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("nodeId") String nodeId,
            @JsonProperty("parentNodeId") String parentNodeId,
            @JsonProperty("nodeType") String nodeType,
            @JsonProperty("status") String status,
            @JsonProperty("output") Map<String, Object> output,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.sequenceNumber = sequenceNumber;
        this.eventVersion = eventVersion;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
        this.nodeType = nodeType;
        this.status = status;
        this.output = output;
        this.metadata = metadata;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RunEventCallbackPayload that)) {
            return false;
        }
        return Objects.equals(sequenceNumber, that.sequenceNumber)
                && Objects.equals(eventVersion, that.eventVersion)
                && Objects.equals(eventType, that.eventType)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(parentNodeId, that.parentNodeId)
                && Objects.equals(nodeType, that.nodeType)
                && Objects.equals(status, that.status)
                && Objects.equals(output, that.output)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sequenceNumber,
                eventVersion,
                eventType,
                correlationId,
                nodeId,
                parentNodeId,
                nodeType,
                status,
                output,
                metadata);
    }
}
