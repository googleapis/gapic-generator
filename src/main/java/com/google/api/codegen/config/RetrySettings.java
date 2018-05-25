/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.config;

import com.google.auto.value.AutoValue;
import java.io.Serializable;
import org.threeten.bp.Duration;

/**
 * Holds the parameters for <b>retry</b> or <b>poll</b> logic with jitter, timeout and exponential
 * backoff. Actual implementation of the logic is elsewhere.
 *
 * <p>This code was copied out of gax-java in order avoid the dependency.
 *
 * <p>The intent of these settings is to be used with a call to a remote server, which could either
 * fail (and return an error code) or not respond (and cause a timeout). When there is a failure or
 * timeout, the logic should keep trying until the total timeout has passed.
 *
 * <p>The "total timeout" parameter has ultimate control over how long the logic should keep trying
 * the remote call until it gives up completely. The higher the total timeout, the more retries can
 * be attempted. The other settings are considered more advanced.
 *
 * <p>Retry delay and timeout start at specific values, and are tracked separately from each other.
 * The very first call (before any retries) will use the initial timeout.
 *
 * <p>If the last remote call is a failure, then the retrier will wait for the current retry delay
 * before attempting another call, and then the retry delay will be multiplied by the retry delay
 * multiplier for the next failure. The timeout will not be affected, except in the case where the
 * timeout would result in a deadline past the total timeout; in that circumstance, a new timeout
 * value is computed which will terminate the call when the total time is up.
 *
 * <p>If the last remote call is a timeout, then the retrier will compute a new timeout and make
 * another call. The new timeout is computed by multiplying the current timeout by the timeout
 * multiplier, but if that results in a deadline after the total timeout, then a new timeout value
 * is computed which will terminate the call when the total time is up.
 */
@AutoValue
public abstract class RetrySettings implements Serializable {

  private static final long serialVersionUID = 8258475264439710899L;

  /**
   * TotalTimeout has ultimate control over how long the logic should keep trying the remote call
   * until it gives up completely. The higher the total timeout, the more retries can be attempted.
   * The default value is {@code Duration.ZERO}.
   */
  public abstract Duration getTotalTimeout();

  /**
   * InitialRetryDelay controls the delay before the first retry. Subsequent retries will use this
   * value adjusted according to the RetryDelayMultiplier. The default value is {@code
   * Duration.ZERO}.
   */
  public abstract Duration getInitialRetryDelay();

  /**
   * RetryDelayMultiplier controls the change in retry delay. The retry delay of the previous call
   * is multiplied by the RetryDelayMultiplier to calculate the retry delay for the next call. The
   * default value is {@code 1.0}.
   */
  public abstract double getRetryDelayMultiplier();

  /**
   * MaxRetryDelay puts a limit on the value of the retry delay, so that the RetryDelayMultiplier
   * can't increase the retry delay higher than this amount. The default value is {@code
   * Duration.ZERO}.
   */
  public abstract Duration getMaxRetryDelay();

  /**
   * MaxAttempts defines the maximum number of attempts to perform. The default value is {@code 0}.
   * If this value is greater than 0, and the number of attempts reaches this limit, the logic will
   * give up retrying even if the total retry time is still lower than TotalTimeout.
   */
  public abstract int getMaxAttempts();

  /**
   * Jitter determines if the delay time should be randomized. In most cases, if jitter is set to
   * {@code true} the actual delay time is calculated in the following way:
   *
   * <pre>{@code actualDelay = rand_between(0, min(maxRetryDelay, delay))}</pre>
   *
   * The default value is {@code true}.
   */
  public abstract boolean isJittered();

  /**
   * InitialRpcTimeout controls the timeout for the initial RPC. Subsequent calls will use this
   * value adjusted according to the RpcTimeoutMultiplier. The default value is {@code
   * Duration.ZERO}.
   */
  public abstract Duration getInitialRpcTimeout();

  /**
   * RpcTimeoutMultiplier controls the change in RPC timeout. The timeout of the previous call is
   * multiplied by the RpcTimeoutMultiplier to calculate the timeout for the next call. The default
   * value is {@code 1.0}.
   */
  public abstract double getRpcTimeoutMultiplier();

  /**
   * MaxRpcTimeout puts a limit on the value of the RPC timeout, so that the RpcTimeoutMultiplier
   * can't increase the RPC timeout higher than this amount. The default value is {@code
   * Duration.ZERO}.
   */
  public abstract Duration getMaxRpcTimeout();

  public static Builder newBuilder() {
    return new AutoValue_RetrySettings.Builder()
        .setTotalTimeout(Duration.ZERO)
        .setInitialRetryDelay(Duration.ZERO)
        .setRetryDelayMultiplier(1.0)
        .setMaxRetryDelay(Duration.ZERO)
        .setMaxAttempts(0)
        .setJittered(true)
        .setInitialRpcTimeout(Duration.ZERO)
        .setRpcTimeoutMultiplier(1.0)
        .setMaxRpcTimeout(Duration.ZERO);
  }

  public abstract Builder toBuilder();

  /**
   * A base builder class for {@link RetrySettings}. See the class documentation of {@link
   * RetrySettings} for a description of the different values that can be set.
   */
  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * TotalTimeout has ultimate control over how long the logic should keep trying the remote call
     * until it gives up completely. The higher the total timeout, the more retries can be
     * attempted. The default value is {@code Duration.ZERO}.
     */
    public abstract Builder setTotalTimeout(Duration totalTimeout);

    /**
     * InitialRetryDelay controls the delay before the first retry. Subsequent retries will use this
     * value adjusted according to the RetryDelayMultiplier. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Builder setInitialRetryDelay(Duration initialDelay);

    /**
     * RetryDelayMultiplier controls the change in retry delay. The retry delay of the previous call
     * is multiplied by the RetryDelayMultiplier to calculate the retry delay for the next call. The
     * default value is {@code 1.0}.
     */
    public abstract Builder setRetryDelayMultiplier(double multiplier);

    /**
     * MaxRetryDelay puts a limit on the value of the retry delay, so that the RetryDelayMultiplier
     * can't increase the retry delay higher than this amount. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Builder setMaxRetryDelay(Duration maxDelay);

    /**
     * MaxAttempts defines the maximum number of attempts to perform. The default value is {@code
     * 0}. If this value is greater than 0, and the number of attempts reaches this limit, the logic
     * will give up retrying even if the total retry time is still lower than TotalTimeout.
     */
    public abstract Builder setMaxAttempts(int maxAttempts);

    /**
     * Jitter determines if the delay time should be randomized. In most cases, if jitter is set to
     * {@code true} the actual delay time is calculated in the following way:
     *
     * <pre>{@code actualDelay = rand_between(0, min(maxRetryDelay, exponentialDelay))}</pre>
     *
     * The default value is {@code true}.
     */
    public abstract Builder setJittered(boolean jittered);

    /**
     * InitialRpcTimeout controls the timeout for the initial RPC. Subsequent calls will use this
     * value adjusted according to the RpcTimeoutMultiplier. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Builder setInitialRpcTimeout(Duration initialTimeout);

    /**
     * See the class documentation of {@link RetrySettings} for a description of what this value
     * does. The default value is {@code 1.0}.
     */
    public abstract Builder setRpcTimeoutMultiplier(double multiplier);

    /**
     * MaxRpcTimeout puts a limit on the value of the RPC timeout, so that the RpcTimeoutMultiplier
     * can't increase the RPC timeout higher than this amount. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Builder setMaxRpcTimeout(Duration maxTimeout);

    /**
     * TotalTimeout has ultimate control over how long the logic should keep trying the remote call
     * until it gives up completely. The higher the total timeout, the more retries can be
     * attempted. The default value is {@code Duration.ZERO}.
     */
    public abstract Duration getTotalTimeout();

    /**
     * InitialRetryDelay controls the delay before the first retry. Subsequent retries will use this
     * value adjusted according to the RetryDelayMultiplier. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Duration getInitialRetryDelay();

    /**
     * RetryDelayMultiplier controls the change in retry delay. The retry delay of the previous call
     * is multiplied by the RetryDelayMultiplier to calculate the retry delay for the next call. The
     * default value is {@code 1.0}.
     */
    public abstract double getRetryDelayMultiplier();

    /**
     * MaxAttempts defines the maximum number of attempts to perform. The default value is {@code
     * 0}. If this value is greater than 0, and the number of attempts reaches this limit, the logic
     * will give up retrying even if the total retry time is still lower than TotalTimeout.
     */
    public abstract int getMaxAttempts();

    /**
     * Jitter determines if the delay time should be randomized. In most cases, if jitter is set to
     * {@code true} the actual delay time is calculated in the following way:
     *
     * <pre>{@code actualDelay = rand_between(0, min(maxRetryDelay, exponentialDelay))}</pre>
     *
     * The default value is {@code true}.
     */
    public abstract boolean isJittered();

    /**
     * MaxRetryDelay puts a limit on the value of the retry delay, so that the RetryDelayMultiplier
     * can't increase the retry delay higher than this amount. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Duration getMaxRetryDelay();

    /**
     * InitialRpcTimeout controls the timeout for the initial RPC. Subsequent calls will use this
     * value adjusted according to the RpcTimeoutMultiplier. The default value is {@code
     * Duration.ZERO}.
     */
    public abstract Duration getInitialRpcTimeout();

    /**
     * See the class documentation of {@link RetrySettings} for a description of what this value
     * does. The default value is {@code 1.0}.
     */
    public abstract double getRpcTimeoutMultiplier();

    /**
     * MaxRpcTimeout puts a limit on the value of the RPC timeout, so that the RpcTimeoutMultiplier
     * can't increase the RPC timeout higher than this amount.
     */
    public abstract Duration getMaxRpcTimeout();

    abstract RetrySettings autoBuild();

    public RetrySettings build() {
      RetrySettings params = autoBuild();
      if (params.getTotalTimeout().toMillis() < 0) {
        throw new IllegalStateException("total timeout must not be negative");
      }
      if (params.getInitialRetryDelay().toMillis() < 0) {
        throw new IllegalStateException("initial retry delay must not be negative");
      }
      if (params.getRetryDelayMultiplier() < 1.0) {
        throw new IllegalStateException("retry delay multiplier must be at least 1");
      }
      if (params.getMaxRetryDelay().compareTo(params.getInitialRetryDelay()) < 0) {
        throw new IllegalStateException("max retry delay must not be shorter than initial delay");
      }
      if (params.getMaxAttempts() < 0) {
        throw new IllegalStateException("max attempts must be non-negative");
      }
      if (params.getInitialRpcTimeout().toMillis() < 0) {
        throw new IllegalStateException("initial rpc timeout must not be negative");
      }
      if (params.getMaxRpcTimeout().compareTo(params.getInitialRpcTimeout()) < 0) {
        throw new IllegalStateException("max rpc timeout must not be shorter than initial timeout");
      }
      if (params.getRpcTimeoutMultiplier() < 1.0) {
        throw new IllegalStateException("rpc timeout multiplier must be at least 1");
      }
      return params;
    }

    public RetrySettings.Builder merge(RetrySettings.Builder newSettings) {
      if (newSettings.getTotalTimeout() != null) {
        setTotalTimeout(newSettings.getTotalTimeout());
      }
      if (newSettings.getInitialRetryDelay() != null) {
        setInitialRetryDelay(newSettings.getInitialRetryDelay());
      }
      if (newSettings.getRetryDelayMultiplier() >= 1) {
        setRetryDelayMultiplier(newSettings.getRetryDelayMultiplier());
      }
      if (newSettings.getMaxRetryDelay() != null) {
        setMaxRetryDelay(newSettings.getMaxRetryDelay());
      }
      setMaxAttempts(newSettings.getMaxAttempts());
      setJittered(newSettings.isJittered());
      if (newSettings.getInitialRpcTimeout() != null) {
        setInitialRpcTimeout(newSettings.getInitialRpcTimeout());
      }
      if (newSettings.getRpcTimeoutMultiplier() >= 1) {
        setRpcTimeoutMultiplier(newSettings.getRpcTimeoutMultiplier());
      }
      if (newSettings.getMaxRpcTimeout() != null) {
        setMaxRpcTimeout(newSettings.getMaxRpcTimeout());
      }
      return this;
    }
  }
}
