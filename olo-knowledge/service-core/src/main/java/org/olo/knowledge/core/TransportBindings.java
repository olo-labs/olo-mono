// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.ololabs.toolgate.contracts.ServiceAudience;
import io.ololabs.toolgate.service.HttpRequestFingerprint;
import io.ololabs.toolgate.service.ServicePermitVerifier;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/** Operator-provisioned bindings. Never obtain profile facts from request headers or claims. */
public final class TransportBindings {
    private final URI origin;
    private final Map<String,HttpRequestFingerprint.Profile> profiles;
    private TransportBindings(URI origin,Map<String,HttpRequestFingerprint.Profile> profiles){
        this.origin=origin;this.profiles=Map.copyOf(profiles);
    }
    public static TransportBindings load(Path file,ServiceAudience audience)throws IOException{
        byte[] bytes;
        try(var input=Files.newInputStream(file)){bytes=input.readNBytes(65537);}
        if(bytes.length>65536)throw invalid();
        var node=Json.MAPPER.readTree(bytes);
        if(node==null||!node.isObject()||!names(node).equals(Set.of("schemaVersion","audience","origin","profileId","profileDigest","routes"))
            ||!node.path("schemaVersion").isIntegralNumber()||node.path("schemaVersion").asLong()!=1)throw invalid();
        String service=audience.name().replace('_','-');
        if(!Json.text(node,"audience",32).equals(service))throw invalid();
        URI origin=URI.create(Json.text(node,"origin",512));
        if(!"https".equals(origin.getScheme())||origin.getHost()==null||origin.getRawUserInfo()!=null
            ||origin.getRawQuery()!=null||origin.getRawFragment()!=null||!(origin.getRawPath().isEmpty()||origin.getRawPath().equals("/"))
            ||origin.getPort()==0||origin.getPort()>65535)throw invalid();
        String profile=Json.text(node,"profileId",128),digest=Json.text(node,"profileDigest",71);
        if(!profile.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}")||!digest.matches("sha256:[a-f0-9]{64}"))throw invalid();
        var routes=node.path("routes");
        if(!routes.isObject()||routes.isEmpty()||routes.size()>16)throw invalid();
        String prefix=audience==ServiceAudience.olo_memory?"memory.":"rag.";
        var allowed=audience==ServiceAudience.olo_memory?Set.of("read","append","delete","share","export"):Set.of("query","ingest","update","delete","share");
        var profiles=new HashMap<String,HttpRequestFingerprint.Profile>();
        for(String operation:names(routes)){
            var value=routes.get(operation);
            if(!operation.startsWith(prefix)||!allowed.contains(operation.substring(prefix.length()))||!value.isTextual()
                ||!value.asText().matches("[a-z][a-z0-9_.-]{0,95}"))throw invalid();
            profiles.put(operation,new HttpRequestFingerprint.Profile(profile,digest,value.asText(),
                "toolgate-service-permit:"+service,List.of("toolgate-service-permit")));
        }
        return new TransportBindings(origin,profiles);
    }
    /** Actual request target, method and headers; public origin only from trusted configuration. */
    public ServicePermitVerifier.Transport facts(String operation,URI requestTarget,String method,Map<String,List<String>> headers){
        var profile=profiles.get(operation);
        if(profile==null||requestTarget.isAbsolute()||requestTarget.getRawAuthority()!=null||requestTarget.getRawFragment()!=null
            ||!requestTarget.getRawPath().equals("/v1/"+operation)||!method.equals("POST"))throw invalid();
        String path=requestTarget.getRawPath()+(requestTarget.getRawQuery()==null?"":"?"+requestTarget.getRawQuery());
        URI actual=URI.create(origin.getScheme()+"://"+origin.getRawAuthority()+path);
        return new ServicePermitVerifier.Transport(profile,actual,method,headers);
    }
    private static Set<String> names(JsonNode node){
        var result=new HashSet<String>();node.fieldNames().forEachRemaining(result::add);return result;
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Invalid trusted HTTP transport binding");}
}
