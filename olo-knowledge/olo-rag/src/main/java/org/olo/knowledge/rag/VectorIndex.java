// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import java.util.*;

public interface VectorIndex {
    record Point(
            String id,
            String tenant,
            String kb,
            String doc,
            long version,
            String model,
            List<Double> vector) {}

    record Reference(String kb, String doc, long version) {}

    record Page(List<Reference> points, String next) {}

    void put(List<Point> points) throws Exception;

    List<String> query(
            String tenant,
            String kb,
            String model,
            List<String> authorizedDocumentVersions,
            List<Double> vector,
            int limit)
            throws Exception;

    void delete(String tenant, String kb, String doc, long version) throws Exception;

    Page scan(String tenant, String cursor) throws Exception;
}
