/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.finagle.scribe;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.twitter.finagle.Name;
import com.twitter.finagle.Resolver$;
import com.twitter.finagle.stats.DefaultStatsReceiver$;
import com.twitter.finagle.stats.NullStatsReceiver;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finagle.tracing.Annotation;
import com.twitter.finagle.tracing.Tracer;
import com.twitter.util.Future;
import com.twitter.util.Time;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;
import zipkin.localServiceName$;
import zipkin2.finagle.ZipkinTracer;

@AutoService(Tracer.class)
public final class ScribeZipkinTracer extends ZipkinTracer {
  private final ScribeSender scribe;

  /**
   * Default constructor for the service loader
   */
  public ScribeZipkinTracer() {
    this(Config.builder().build(), DefaultStatsReceiver$.MODULE$.get().scope("zipkin.scribe"));
  }

  ScribeZipkinTracer(Config config, StatsReceiver stats) {
    this(new ScribeSender(config), config, stats);
  }

  private ScribeZipkinTracer(ScribeSender scribe, Config config, StatsReceiver stats) {
    super(scribe, config, stats);
    this.scribe = scribe;
  }

  /**
   * Create a new instance with default configuration.
   *
   * @param host The network location of the Zipkin scribe service
   * @param stats Gets notified when spans are accepted or dropped. If you are not interested in
   * these events you can use {@linkplain NullStatsReceiver}
   */
  public static ScribeZipkinTracer create(String host, StatsReceiver stats) {
    return new ScribeZipkinTracer(Config.builder().host(host).build(), stats);
  }

  /**
   * @param config includes flush interval and scribe properties
   * @param stats Gets notified when spans are accepted or dropped. If you are not interested in
   * these events you can use {@linkplain NullStatsReceiver}
   */
  public static ScribeZipkinTracer create(Config config, StatsReceiver stats) {
    return new ScribeZipkinTracer(config, stats);
  }

  @Override public Future<BoxedUnit> close(final Time deadline) {
    return scribe.closeFuture().flatMap(new AbstractFunction1<BoxedUnit, Future<BoxedUnit>>() {
      @Override public Future<BoxedUnit> apply(BoxedUnit v1) {
        return ScribeZipkinTracer.super.close(deadline);
      }
    });
  }

  @AutoValue
  public static abstract class Config implements ZipkinTracer.Config {
    /** Creates a builder with the correct defaults derived from global flags */
    public static Builder builder() {
      return new AutoValue_ScribeZipkinTracer_Config.Builder()
          .host(zipkin.scribe.host$.Flag.apply())
          .localServiceName(localServiceName$.Flag.apply())
          .initialSampleRate(zipkin.initialSampleRate$.Flag.apply());
    }

    abstract public Builder toBuilder();

    abstract Name host();

    @AutoValue.Builder
    public abstract static class Builder {
      /**
       * Lower-case label of the remote node in the service graph, such as "favstar". Avoid names
       * with variables or unique identifiers embedded.
       *
       * <p>When unset, the service name is derived from {@link Annotation.ServiceName} which is
       * often incorrectly set to the remote service name.
       *
       * <p>This is a primary label for trace lookup and aggregation, so it should be intuitive and
       * consistent. Many use a name from service discovery.
       */
      public abstract Builder localServiceName(String localServiceName);

      /** The network location of the Scribe service. Defaults to "localhost:1463" */
      public abstract Builder host(Name host);

      /** Shortcut for a {@link #host(Name)} encoded as a String */
      public final Builder host(String host) {
        return host(Resolver$.MODULE$.eval(host));
      }

      /** @see ZipkinTracer.Config#initialSampleRate() */
      public abstract Builder initialSampleRate(float initialSampleRate);

      public abstract Config build();
    }
  }
}
