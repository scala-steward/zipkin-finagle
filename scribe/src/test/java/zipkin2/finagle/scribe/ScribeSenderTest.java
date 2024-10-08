/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.finagle.scribe;

import org.apache.thrift.TException;
import org.junit.Test;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.finagle.FinagleTestObjects;
import zipkin2.finagle.scribe.ScribeZipkinTracer.Config;

import static java.util.Arrays.asList;

public class ScribeSenderTest {

  Config config = Config.builder().initialSampleRate(1.0f).host("127.0.0.1:9410").build();

  @Test
  public void makeRequest() throws TException {
    Endpoint web = Endpoint.newBuilder().serviceName("web").ip("127.0.0.1").build();
    Span span1 =
        Span.newBuilder()
            .traceId(FinagleTestObjects.root.traceId().toString())
            .id(FinagleTestObjects.root.spanId().toLong())
            .name("get")
            .timestamp(FinagleTestObjects.TODAY * 1000)
            .duration(1000L)
            .kind(Span.Kind.SERVER)
            .localEndpoint(web)
            .build();

    Span span2 =
        span1
            .toBuilder()
            .kind(Span.Kind.CLIENT)
            .parentId(FinagleTestObjects.child.parentId().toLong())
            .id(FinagleTestObjects.child.spanId().toLong())
            .build();

    new ScribeSender(config)
        .makeRequest(
            asList(SpanBytesEncoder.THRIFT.encode(span1), SpanBytesEncoder.THRIFT.encode(span2)));
  }
}
