/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

@OloTool(
        id = CoreHumanInputPluginIds.BOOK_TICKET,
        name = "Book Ticket Human Input",
        description = "Collects operator approval and book-ticket arguments before the mock action runs",
        category = "human-input",
        emoji = "🎫",
        tags = {"human-input", "plugin", "travel", "booking"},
        examples = {"Approve booking offer paris-weekend-001 for Alex Operator"},
        arguments = {
            @OloProperty(
                    name = "approveBooking",
                    label = "Approve booking?",
                    type = OloPropertyType.APPROVAL_TOGGLE,
                    required = true,
                    description = "Select Yes to authorize the ticket booking",
                    group = "Approval",
                    order = 0),
            @OloProperty(
                    name = "scopeNotes",
                    label = "Trip notes",
                    type = OloPropertyType.TEXTAREA,
                    description = "Optional preferences or constraints for the booking",
                    group = "Scope",
                    order = 1),
            @OloProperty(
                    name = "offerId",
                    label = "Offer ID",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Travel offer identifier to book",
                    placeholder = "paris-weekend-001",
                    group = "Booking action",
                    order = 2),
            @OloProperty(
                    name = "passengerName",
                    label = "Passenger name",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Passenger name for the ticket",
                    placeholder = "Alex Operator",
                    group = "Booking action",
                    order = 3)
        })
@ToolId(CoreHumanInputPluginIds.BOOK_TICKET)
@ImplementationId(CoreHumanInputPluginIds.BOOK_TICKET)
public final class BookTicketHumanInputPlugin implements Tool {

    @Override
    public String toolId() {
        return CoreHumanInputPluginIds.BOOK_TICKET;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        return HumanInputPluginSupport.schemaOnlyInvoke(toolId(), request, context);
    }
}
