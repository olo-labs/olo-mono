/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.core.tool.action.MockActionLogSupport;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@OloTool(
        id = CoreToolIds.BOOK_TICKET,
        name = "Book Ticket",
        description = "Mock travel ticket booking after human approval (writes execution log)",
        category = "action",
        emoji = "🎫",
        tags = {"travel", "booking", "human-approved", "mock"},
        examples = {
            "Book offer LON-PAR-42 for passenger Alex Morgan",
            "Confirm weekend package offer EU-SUMMER-7 after itinerary review"
        },
        arguments = {
            @OloProperty(
                    name = "offerId",
                    label = "Offer ID",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Travel offer identifier from travel-offers tool",
                    placeholder = "LON-PAR-42",
                    group = "Booking",
                    order = 0),
            @OloProperty(
                    name = "passengerName",
                    label = "Passenger Name",
                    type = OloPropertyType.STRING,
                    required = true,
                    description = "Primary passenger name",
                    placeholder = "Alex Morgan",
                    group = "Booking",
                    order = 1),
            @OloProperty(
                    name = "email",
                    label = "Contact Email",
                    type = OloPropertyType.STRING,
                    description = "Booking confirmation email",
                    placeholder = "alex@example.com",
                    group = "Booking",
                    order = 2)
        })
@ToolId(CoreToolIds.BOOK_TICKET)
@ImplementationId(CoreToolIds.BOOK_TICKET)
public final class BookTicketTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.BOOK_TICKET;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String offerId = ToolArgs.string(request.arguments(), "offerId", "");
        String passengerName = ToolArgs.string(request.arguments(), "passengerName", "");
        if (offerId.isBlank() || passengerName.isBlank()) {
            return ToolResult.failure("offerId and passengerName are required for book-ticket mock action", null);
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("offerId", offerId);
        arguments.put("passengerName", passengerName);
        String email = ToolArgs.string(request.arguments(), "email", "");
        if (!email.isBlank()) {
            arguments.put("email", email);
        }
        String bookingReference = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            Map<String, Object> output =
                    MockActionLogSupport.recordMockExecution(context, toolId(), "BOOK_TICKET", arguments);
            output.put("bookingReference", bookingReference);
            output.put("bookingStatus", "CONFIRMED");
            output.put("summary", "Mock ticket booked for " + passengerName + " on offer " + offerId);
            return ToolResult.success(output);
        } catch (Exception e) {
            return ToolResult.failure("Book ticket mock action failed: " + e.getMessage(), e);
        }
    }
}
