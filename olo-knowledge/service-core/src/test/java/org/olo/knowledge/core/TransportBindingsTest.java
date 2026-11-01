// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import static org.junit.jupiter.api.Assertions.*;
import io.ololabs.toolgate.contracts.ServiceAudience;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransportBindingsTest {
    @TempDir Path directory;
    private Map<String,Object> config(){
        return Map.of("schemaVersion",1,"audience","olo-memory","origin","https://memory.test:8443",
            "profileId","sp-olo.memory","profileDigest","sha256:"+"a".repeat(64),
            "routes",Map.of("memory.append","append","memory.read","read"));
    }
    private Path write(Object config)throws Exception{
        var file=directory.resolve("bindings.json");Files.writeString(file,Json.encode(config));return file;
    }
    @Test void actualRequestFactsUseOnlyTheConfiguredPublicOrigin()throws Exception{
        var bindings=TransportBindings.load(write(config()),ServiceAudience.olo_memory);
        var facts=bindings.facts("memory.append",URI.create("/v1/memory.append?a=2&a=1"),"POST",
            Map.of("Host",List.of("evil.test"),"Content-Type",List.of("application/json")));
        assertEquals("https://memory.test:8443/v1/memory.append?a=2&a=1",facts.uri().toString());
        assertEquals("append",facts.profile().route());
        assertThrows(UnsupportedOperationException.class,()->facts.headers().clear());
        for(String target:List.of("https://evil.test/v1/memory.append","//evil.test/v1/memory.append","/v1/memory.delete")){
            assertThrows(IllegalArgumentException.class,()->bindings.facts("memory.append",URI.create(target),"POST",Map.of()));
        }
        assertThrows(IllegalArgumentException.class,()->bindings.facts("memory.append",URI.create("/v1/memory.append"),"DELETE",Map.of()));
    }
    @Test void malformedWrongAudienceAndUntrustedConfigurationFailClosed()throws Exception{
        for(var change:List.<Map.Entry<String,Object>>of(Map.entry("origin","http://memory.test"),
            Map.entry("origin","https://user@memory.test"),Map.entry("origin","https://memory.test/path"),
            Map.entry("profileDigest","sha256:"+"f".repeat(63)),Map.entry("audience","olo-rag"),Map.entry("unexpected","value"))){
            var c=new HashMap<>(config());c.put(change.getKey(),change.getValue());var file=write(c);
            assertThrows(IllegalArgumentException.class,()->TransportBindings.load(file,ServiceAudience.olo_memory));
        }
        var file=directory.resolve("duplicate.json");Files.writeString(file,"{\"schemaVersion\":1,\"schemaVersion\":1}");
        assertThrows(java.io.IOException.class,()->TransportBindings.load(file,ServiceAudience.olo_memory));
    }
}
