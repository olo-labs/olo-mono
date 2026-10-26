// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.toolgate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class GovernedCallsReplayTest {
    @Test
    void equivalentMapsFromDifferentProcessesProduceTheSameInvocationBody() {
        var bodies = new ArrayList<JsonNode>();
        var ids = new ArrayList<String>();
        var caller = new ToolGateClient() {
            public ToolGateClient forUser(UserBinding user) { return this; }
            public List<ToolGateTool> listTools(String name) { throw new AssertionError("No catalog on path A"); }
            public void probe() {}
            public ToolGateCallResult callTool(String name, String id, String tool, JsonNode arguments) {
                bodies.add(arguments.deepCopy()); ids.add(id);
                return new ToolGateCallResult(false,"",new ObjectMapper().createObjectNode().put("json","{}"),null,null);
            }
        };
        var forward = new LinkedHashMap<String,Object>();
        forward.put("content","private text");
        forward.put("dependencies",List.of(new LinkedHashMap<>(Map.of("version",1,"docId","doc"))));
        var reverse = new LinkedHashMap<String,Object>();
        var nested = new LinkedHashMap<String,Object>(); nested.put("docId","doc"); nested.put("version",1);
        reverse.put("dependencies",List.of(nested)); reverse.put("content","private text");
        var user = new UserBinding("tenant","user","issuer","session",1);
        String resource=UUID.randomUUID().toString();
        new GovernedCalls(caller).user(user,"original","memory.append",resource,forward);
        new GovernedCalls(caller).user(user,"original","memory.append",resource,reverse);
        assertThat(ids.get(0)).isEqualTo(ids.get(1));
        assertThat(bodies.get(0)).isEqualTo(bodies.get(1));
    }
}
